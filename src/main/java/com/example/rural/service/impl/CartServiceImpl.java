package com.example.rural.service.impl;

import com.example.rural.dto.request.CartAddRequest;
import com.example.rural.dto.request.CartUpdateRequest;
import com.example.rural.dto.response.CartItemResponse;
import com.example.rural.dto.response.CartResponse;
import com.example.rural.entity.CartItem;
import com.example.rural.entity.Product;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.CartItemRepository;
import com.example.rural.repository.ProductRepository;
import com.example.rural.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车服务实现
 *
 * 数据流:
 *
 *   前端「加入购物车」按钮
 *     ↓
 *   CartAddRequest { productId:1, quantity:2 }
 *     ↓
 *   CartService.addToCart()
 *     ↓ 查 product 表 → 校验在售+库存
 *     ↓ 查 cart_item 表 → 已有就累加，没有就新建
 *     ↓
 *   cart_item 表（持久化到MySQL，换设备登录也在）
 *     ↓
 *   CartItemResponse → 前端更新购物车UI
 *
 * 查看购物车时:
 *   cart_item 表 + product 表 JOIN
 *   → 实时获取最新价格和库存（不缓存旧价格）
 *   → 如果商品已下架或库存不足，标记 available=false
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    /**
     * 添加商品到购物车
     *
     * 核心逻辑:
     *   1. 校验商品存在且在售
     *   2. 查购物车是否已有该商品
     *      - 已有 → 累加数量
     *      - 没有 → 新建条目
     *   3. 校验总数量不超过库存
     */
    @Override
    @Transactional
    public CartItemResponse addToCart(Long userId, CartAddRequest request) {
        // 1. 校验商品
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));

        if (product.getStatus() != Product.Status.ON_SALE) {
            throw new BusinessException("商品已下架，无法加入购物车");
        }

        // 2. 查已有条目
        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(userId, request.getProductId())
                .orElse(null);

        if (cartItem != null) {
            // 累加数量
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            if (newQuantity > product.getStock()) {
                throw new BusinessException("库存不足，当前库存: " + product.getStock()
                        + "，购物车已有: " + cartItem.getQuantity());
            }
            cartItem.setQuantity(newQuantity);
        } else {
            // 新建
            if (request.getQuantity() > product.getStock()) {
                throw new BusinessException("库存不足，当前库存: " + product.getStock());
            }
            cartItem = CartItem.builder()
                    .userId(userId)
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .selected(true)
                    .build();
        }

        CartItem saved = cartItemRepository.save(cartItem);
        log.info("购物车更新: userId={}, productId={}, quantity={}",
                userId, product.getId(), saved.getQuantity());

        return toCartItemResponse(saved, product);
    }

    /**
     * 查看购物车
     *
     * 实时查商品表获取最新价格和库存，
     * 如果商品已被删除/下架，仍然展示但标记为不可购买。
     */
    @Override
    public CartResponse getMyCart(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<CartItemResponse> items = cartItems.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    return toCartItemResponse(item, product);
                })
                .toList();

        // 计算汇总
        BigDecimal selectedAmount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getSelected()) && Boolean.TRUE.equals(i.getAvailable()))
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long selectedCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getSelected()))
                .count();

        boolean allSelected = !items.isEmpty() && items.stream().allMatch(
                i -> Boolean.TRUE.equals(i.getSelected()));

        return CartResponse.builder()
                .items(items)
                .totalCount(items.size())
                .selectedCount((int) selectedCount)
                .selectedAmount(selectedAmount)
                .allSelected(allSelected)
                .build();
    }

    @Override
    @Transactional
    public CartItemResponse updateCartItem(Long userId, Long cartItemId, CartUpdateRequest request) {
        CartItem cartItem = findCartItemOrThrow(userId, cartItemId);

        if (request.getQuantity() != null) {
            // 校验库存
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new BusinessException("商品已被删除"));
            if (request.getQuantity() > product.getStock()) {
                throw new BusinessException("库存不足，当前库存: " + product.getStock());
            }
            cartItem.setQuantity(request.getQuantity());
        }

        if (request.getSelected() != null) {
            cartItem.setSelected(request.getSelected());
        }

        CartItem saved = cartItemRepository.save(cartItem);
        Product product = productRepository.findById(saved.getProductId()).orElse(null);
        return toCartItemResponse(saved, product);
    }

    @Override
    @Transactional
    public void selectAll(Long userId, boolean selected) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        items.forEach(item -> item.setSelected(selected));
        cartItemRepository.saveAll(items);
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = findCartItemOrThrow(userId, cartItemId);
        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Override
    public long getCartCount(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }

    // ======================== 工具方法 ========================

    private CartItem findCartItemOrThrow(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(404, "购物车条目不存在"));
        if (!cartItem.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此购物车条目");
        }
        return cartItem;
    }

    /**
     * CartItem + Product → CartItemResponse
     *
     * 关键: 价格从 product 表实时取（不是存在购物车里的旧价格），
     * 这样商家调价后用户看到的是最新价。
     */
    private CartItemResponse toCartItemResponse(CartItem cartItem, Product product) {
        if (product == null) {
            // 商品已被删除
            return CartItemResponse.builder()
                    .id(cartItem.getId())
                    .productId(cartItem.getProductId())
                    .productName("[商品已删除]")
                    .quantity(cartItem.getQuantity())
                    .selected(cartItem.getSelected())
                    .subtotal(BigDecimal.ZERO)
                    .available(false)
                    .createdAt(cartItem.getCreatedAt())
                    .build();
        }

        boolean available = product.getStatus() == Product.Status.ON_SALE
                && product.getStock() >= cartItem.getQuantity();

        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .price(product.getPrice())
                .unit(product.getUnit())
                .stock(product.getStock())
                .productStatus(product.getStatus().name())
                .quantity(cartItem.getQuantity())
                .selected(cartItem.getSelected())
                .subtotal(subtotal)
                .available(available)
                .createdAt(cartItem.getCreatedAt())
                .build();
    }
}
