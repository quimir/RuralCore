package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.OrderCreateRequest;
import com.example.rural.dto.response.OrderResponse;

/**
 * 订单服务接口
 *
 * 下单流程（核心事务）:
 *
 *   1. 读取购物车中「已选中」的条目
 *   2. 逐一校验商品: 在售? 库存够?
 *   3. 生成订单号
 *   4. 创建 orders 记录（订单头）
 *   5. 创建 order_item 记录（快照商品信息）
 *   6. 扣减库存（product.stock -= quantity）
 *   7. 累加销量（product.sales_count += quantity）
 *   8. 清除购物车中已下单的条目
 *   9. 以上操作在同一个事务中，任一步失败全部回滚
 */
public interface OrderService {

    /** 从购物车选中商品下单 */
    OrderResponse createOrder(Long buyerId, OrderCreateRequest request);

    /** 查看订单详情 */
    OrderResponse getOrderDetail(Long orderId, Long currentUserId);

    /** 我的订单列表（买家视角，支持按状态筛选） */
    PageResult<OrderResponse> getMyOrders(Long buyerId, String status, int page, int size);

    /** 模拟支付（真实项目对接支付宝/微信） */
    void payOrder(Long orderId, Long buyerId);

    /** 卖家发货 */
    void shipOrder(Long orderId, Long sellerId);

    /** 买家确认收货 */
    void confirmReceive(Long orderId, Long buyerId);

    /** 取消订单（仅待付款状态可取消） */
    void cancelOrder(Long orderId, Long buyerId);
}
