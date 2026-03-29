package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.OrderCreateRequest;
import com.example.rural.dto.response.OrderResponse;
import com.example.rural.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 *
 * 所有接口需要登录（Bearer Token）
 *
 * ┌────────────────────────────────────────────────────────────────────┐
 * │  完整下单链路                                                      │
 * │                                                                    │
 * │  1. 浏览商品        GET  /api/v1/products（公开）                   │
 * │  2. 加入购物车      POST /api/v1/cart                              │
 * │  3. 查看购物车      GET  /api/v1/cart                              │
 * │  4. 勾选/改数量     PUT  /api/v1/cart/{id}                         │
 * │  5. 去结算（下单）  POST /api/v1/orders                            │
 * │  6. 支付           PUT  /api/v1/orders/{id}/pay                   │
 * │  7. 卖家发货       PUT  /api/v1/orders/{id}/ship                  │
 * │  8. 确认收货       PUT  /api/v1/orders/{id}/confirm               │
 * └────────────────────────────────────────────────────────────────────┘
 *
 * 订单状态: PENDING_PAYMENT → PAID → SHIPPED → COMPLETED
 *                  ↓               ↓
 *              CANCELLED       CANCELLED（需退款）
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 下单（从购物车选中商品结算）
     *
     * 方式1（推荐）: POST /api/v1/orders
     *   { "addressId": 1, "remark": "请周末配送" }
     *
     * 方式2（兼容旧版）: POST /api/v1/orders
     *   { "shippingAddress": "浙江省...", "receiverName": "张三",
     *     "receiverPhone": "13800138000", "remark": "..." }
     *
     * addressId 优先; 两种都不传则返回错误。
     */
    @PostMapping
    public Result<OrderResponse> createOrder(@RequestBody OrderCreateRequest request,
                                              Authentication auth) {
        Long buyerId = (Long) auth.getPrincipal();
        return Result.ok("下单成功", orderService.createOrder(buyerId, request));
    }

    /**
     * 查看订单详情
     *
     * 请求: GET /api/v1/orders/1
     * 买家和相关卖家均可查看
     */
    @GetMapping("/{id}")
    public Result<OrderResponse> getOrderDetail(@PathVariable Long id,
                                                 Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        return Result.ok(orderService.getOrderDetail(id, currentUserId));
    }

    /**
     * 我的订单列表
     *
     * 请求:
     *   GET /api/v1/orders?page=1&size=10                  → 全部订单
     *   GET /api/v1/orders?status=PENDING_PAYMENT&page=1   → 待付款
     *   GET /api/v1/orders?status=PAID&page=1              → 待发货
     *   GET /api/v1/orders?status=SHIPPED&page=1           → 待收货
     */
    @GetMapping
    public Result<PageResult<OrderResponse>> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long buyerId = (Long) auth.getPrincipal();
        return Result.ok(orderService.getMyOrders(buyerId, status, page, size));
    }

    /**
     * 模拟支付
     *
     * 请求: PUT /api/v1/orders/1/pay
     *
     * 真实项目中这里会:
     *   1. 前端调用支付宝/微信SDK
     *   2. 用户完成支付
     *   3. 支付平台回调后端
     *   4. 后端更新订单状态
     * 这里简化为直接调接口
     */
    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id, Authentication auth) {
        Long buyerId = (Long) auth.getPrincipal();
        orderService.payOrder(id, buyerId);
        return Result.ok("支付成功", null);
    }

    /**
     * 卖家发货
     *
     * 请求: PUT /api/v1/orders/1/ship
     */
    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable Long id, Authentication auth) {
        Long sellerId = (Long) auth.getPrincipal();
        orderService.shipOrder(id, sellerId);
        return Result.ok("已发货", null);
    }

    /**
     * 买家确认收货
     *
     * 请求: PUT /api/v1/orders/1/confirm
     */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmReceive(@PathVariable Long id, Authentication auth) {
        Long buyerId = (Long) auth.getPrincipal();
        orderService.confirmReceive(id, buyerId);
        return Result.ok("已确认收货", null);
    }

    /**
     * 取消订单（仅待付款可取消，会恢复库存）
     *
     * 请求: PUT /api/v1/orders/1/cancel
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id, Authentication auth) {
        Long buyerId = (Long) auth.getPrincipal();
        orderService.cancelOrder(id, buyerId);
        return Result.ok("订单已取消", null);
    }
}
