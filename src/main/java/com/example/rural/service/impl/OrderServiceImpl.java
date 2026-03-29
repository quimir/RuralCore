package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.OrderCreateRequest;
import com.example.rural.dto.response.OrderResponse;
import com.example.rural.entity.*;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.*;
import com.example.rural.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务实现
 *
 * 下单是整个系统最关键的事务操作，涉及多张表的一致性:
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │  @Transactional — 一个事务内完成，失败全部回滚        │
 *   │                                                     │
 *   │  1. 读购物车选中项                                   │
 *   │  2. 校验每个商品（在售 + 库存够）                     │
 *   │  3. 生成订单号                                       │
 *   │  4. 创建 orders 记录                                 │
 *   │  5. 创建 order_item（快照商品名/价格/图片）           │
 *   │  6. product.stock -= quantity（扣库存）               │
 *   │  7. product.sales_count += quantity（加销量）         │
 *   │  8. 删除购物车已下单条目                              │
 *   └─────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    // ======================== 下单 ========================

    @Override
    @Transactional
    public OrderResponse createOrder(Long buyerId, OrderCreateRequest request) {

        // 1. 获取购物车中已选中的条目
        List<CartItem> selectedItems = cartItemRepository.findByUserIdAndSelectedTrue(buyerId);
        if (selectedItems.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }

        // 2. 校验每个商品并计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : selectedItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new BusinessException(
                            "商品已被删除: " + cartItem.getProductId()));

            if (product.getStatus() != Product.Status.ON_SALE) {
                throw new BusinessException("商品「" + product.getName() + "」已下架");
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("商品「" + product.getName()
                        + "」库存不足，剩余: " + product.getStock());
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // 3. 生成订单号
        String orderNo = generateOrderNo();

        // 4. 解析收货地址（addressId 优先 → 手动输入兜底）
        String shippingAddress;
        String receiverName;
        String receiverPhone;
        Long addressId = null;

        if (request.getAddressId() != null) {
            // 方式1: 从地址簿读取
            Address addr = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new BusinessException("地址不存在"));
            if (!addr.getUserId().equals(buyerId)) {
                throw new BusinessException(403, "无权使用此地址");
            }
            addressId = addr.getId();
            shippingAddress = addr.getFullAddress();
            receiverName = addr.getReceiverName();
            receiverPhone = addr.getReceiverPhone();
        } else {
            // 方式2: 手动输入（兼容旧版）
            if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
                throw new BusinessException("请选择收货地址或填写收货地址");
            }
            if (request.getReceiverName() == null || request.getReceiverName().isBlank()) {
                throw new BusinessException("收货人姓名不能为空");
            }
            if (request.getReceiverPhone() == null || request.getReceiverPhone().isBlank()) {
                throw new BusinessException("收货人电话不能为空");
            }
            shippingAddress = request.getShippingAddress();
            receiverName = request.getReceiverName();
            receiverPhone = request.getReceiverPhone();
        }

        // 5. 创建订单（地址信息快照到订单，后续地址簿修改不影响）
        Order order = Order.builder()
                .orderNo(orderNo)
                .buyerId(buyerId)
                .totalAmount(totalAmount)
                .status(Order.Status.PENDING_PAYMENT)
                .addressId(addressId)
                .shippingAddress(shippingAddress)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .remark(request.getRemark())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 6. 创建订单明细 + 扣库存 + 加销量 + 加购买人次
        for (CartItem cartItem : selectedItems) {
            Product product = productRepository.findById(cartItem.getProductId()).orElseThrow();

            // 创建明细（快照商品信息）
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getId())
                    .productId(product.getId())
                    .sellerId(product.getSellerId())
                    .productName(product.getName())
                    .productImage(product.getImageUrl())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .unit(product.getUnit())
                    .subtotal(subtotal)
                    .build();
            orderItemRepository.save(orderItem);

            // 扣库存 + 加销量 + 加购买人次
            product.setStock(product.getStock() - cartItem.getQuantity());
            product.setSalesCount(product.getSalesCount() + cartItem.getQuantity());
            product.setBuyerCount(product.getBuyerCount() + 1);  // 每笔订单含该商品 +1
            if (product.getStock() == 0) {
                product.setStatus(Product.Status.SOLD_OUT);
            }
            productRepository.save(product);
        }

        // 7. 清除已下单的购物车条目
        List<Long> cartItemIds = selectedItems.stream().map(CartItem::getId).toList();
        cartItemRepository.deleteAllById(cartItemIds);

        log.info("订单创建成功: orderNo={}, buyerId={}, totalAmount={}, items={}",
                orderNo, buyerId, totalAmount, selectedItems.size());

        return toOrderResponse(savedOrder);
    }

    // ======================== 查询 ========================

    @Override
    public OrderResponse getOrderDetail(Long orderId, Long currentUserId) {
        Order order = findOrderOrThrow(orderId);

        // 买家或相关卖家可查看
        if (!order.getBuyerId().equals(currentUserId)) {
            // 检查是否是卖家（订单明细中包含该卖家的商品）
            boolean isSeller = orderItemRepository.findByOrderId(orderId).stream()
                    .anyMatch(item -> item.getSellerId().equals(currentUserId));
            if (!isSeller) {
                throw new BusinessException(403, "无权查看此订单");
            }
        }

        return toOrderResponse(order);
    }

    @Override
    public PageResult<OrderResponse> getMyOrders(Long buyerId, String status,
                                                  int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);

        Page<Order> orderPage;
        if (status != null && !status.isBlank()) {
            try {
                Order.Status statusEnum = Order.Status.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(
                        buyerId, statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的订单状态: " + status);
            }
        } else {
            orderPage = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
        }

        return PageResult.from(orderPage, this::toOrderResponse);
    }

    // ======================== 状态流转 ========================

    /**
     * 模拟支付
     *
     * 真实项目中这里会对接支付宝/微信支付回调:
     *   1. 前端调用支付SDK → 支付平台
     *   2. 支付平台回调后端 → 更新订单状态
     *   这里简化为直接调接口标记已支付
     */
    @Override
    @Transactional
    public void payOrder(Long orderId, Long buyerId) {
        Order order = findOrderOrThrow(orderId);
        checkBuyer(order, buyerId);

        if (order.getStatus() != Order.Status.PENDING_PAYMENT) {
            throw new BusinessException("当前订单状态不允许支付");
        }

        order.setStatus(Order.Status.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("订单已支付: orderNo={}", order.getOrderNo());
    }

    /**
     * 卖家发货
     *
     * 校验: 订单明细中必须包含该卖家的商品
     */
    @Override
    @Transactional
    public void shipOrder(Long orderId, Long sellerId) {
        Order order = findOrderOrThrow(orderId);

        // 验证是否为相关卖家
        boolean isSeller = orderItemRepository.findByOrderId(orderId).stream()
                .anyMatch(item -> item.getSellerId().equals(sellerId));
        if (!isSeller) {
            throw new BusinessException(403, "此订单不包含您的商品");
        }

        if (order.getStatus() != Order.Status.PAID) {
            throw new BusinessException("只有已付款的订单可以发货");
        }

        order.setStatus(Order.Status.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("订单已发货: orderNo={}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void confirmReceive(Long orderId, Long buyerId) {
        Order order = findOrderOrThrow(orderId);
        checkBuyer(order, buyerId);

        if (order.getStatus() != Order.Status.SHIPPED) {
            throw new BusinessException("只有已发货的订单可以确认收货");
        }

        order.setStatus(Order.Status.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("订单已完成: orderNo={}", order.getOrderNo());
    }

    /**
     * 取消订单
     *
     * 仅待付款状态可取消，取消后恢复库存
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long buyerId) {
        Order order = findOrderOrThrow(orderId);
        checkBuyer(order, buyerId);

        if (order.getStatus() != Order.Status.PENDING_PAYMENT) {
            throw new BusinessException("只有待付款的订单可以取消");
        }

        // 恢复库存
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                product.setSalesCount(Math.max(0, product.getSalesCount() - item.getQuantity()));
                product.setBuyerCount(Math.max(0, product.getBuyerCount() - 1));
                if (product.getStatus() == Product.Status.SOLD_OUT && product.getStock() > 0) {
                    product.setStatus(Product.Status.ON_SALE);
                }
                productRepository.save(product);
            }
        }

        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        log.info("订单已取消: orderNo={}, 库存已恢复", order.getOrderNo());
    }

    // ======================== 工具方法 ========================

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }

    private void checkBuyer(Order order, Long buyerId) {
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(403, "无权操作此订单");
        }
    }

    /**
     * 生成订单号: yyyyMMddHHmmss + 6位随机数
     * 例: 20260211143052839271
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return timestamp + random;
    }

    /**
     * Order → OrderResponse（附带明细和买家/卖家名称）
     */
    private OrderResponse toOrderResponse(Order order) {
        // 查买家名
        String buyerName = userRepository.findById(order.getBuyerId())
                .map(User::getNickname)
                .orElse("未知");

        // 查订单明细
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderResponse.OrderItemResponse> itemResponses = items.stream()
                .map(item -> {
                    String sellerName = userRepository.findById(item.getSellerId())
                            .map(User::getNickname)
                            .orElse("未知");

                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .productImage(item.getProductImage())
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .unit(item.getUnit())
                            .subtotal(item.getSubtotal())
                            .sellerId(item.getSellerId())
                            .sellerName(sellerName)
                            .build();
                }).toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .addressId(order.getAddressId())
                .shippingAddress(order.getShippingAddress())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .remark(order.getRemark())
                .buyerId(order.getBuyerId())
                .buyerName(buyerName)
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .completedAt(order.getCompletedAt())
                .items(itemResponses)
                .build();
    }
}
