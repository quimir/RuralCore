package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.dto.request.CartAddRequest;
import com.example.rural.dto.request.CartUpdateRequest;
import com.example.rural.dto.response.CartItemResponse;
import com.example.rural.dto.response.CartResponse;
import com.example.rural.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车控制器
 *
 * 所有接口需要登录（Bearer Token）
 *
 * ┌────────────────────────────────────────────────────────────────────┐
 * │  接口一览                                                          │
 * │                                                                    │
 * │  POST   /api/v1/cart              加入购物车（已有则累加数量）       │
 * │  GET    /api/v1/cart              查看我的购物车                    │
 * │  PUT    /api/v1/cart/{id}         修改数量/选中状态                 │
 * │  PUT    /api/v1/cart/select-all   全选/取消全选                     │
 * │  DELETE /api/v1/cart/{id}         删除单个条目                      │
 * │  DELETE /api/v1/cart              清空购物车                        │
 * │  GET    /api/v1/cart/count        购物车数量（角标）                │
 * └────────────────────────────────────────────────────────────────────┘
 *
 * 前端数据流:
 *
 *   商品详情页 [加入购物车] → POST /api/v1/cart
 *                              ↓
 *   购物车页面 ← GET /api/v1/cart （含汇总: 已选商品总价、是否全选等）
 *                              ↓
 *   修改数量 / 勾选 → PUT /api/v1/cart/{id}
 *                              ↓
 *   [去结算] → POST /api/v1/orders （结算选中的商品）
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 加入购物车
     *
     * 请求: POST /api/v1/cart
     * Body: { "productId": 1, "quantity": 2 }
     *
     * 如果购物车已有该商品，数量会自动累加
     */
    @PostMapping
    public Result<CartItemResponse> addToCart(@Valid @RequestBody CartAddRequest request,
                                              Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("已加入购物车", cartService.addToCart(userId, request));
    }

    /**
     * 查看我的购物车
     *
     * 请求: GET /api/v1/cart
     *
     * 返回购物车全部条目 + 汇总信息（已选总价、商品数等）
     * 商品价格和库存是实时从 product 表获取的最新值
     */
    @GetMapping
    public Result<CartResponse> getMyCart(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(cartService.getMyCart(userId));
    }

    /**
     * 修改购物车条目
     *
     * 请求: PUT /api/v1/cart/5
     * Body: { "quantity": 3 }          → 修改数量
     *   或: { "selected": false }      → 取消勾选
     *   或: { "quantity": 5, "selected": true }
     */
    @PutMapping("/{id}")
    public Result<CartItemResponse> updateCartItem(@PathVariable Long id,
                                                    @Valid @RequestBody CartUpdateRequest request,
                                                    Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(cartService.updateCartItem(userId, id, request));
    }

    /**
     * 全选 / 取消全选
     *
     * 请求: PUT /api/v1/cart/select-all
     * Body: { "selected": true }   → 全选
     *   或: { "selected": false }  → 取消全选
     */
    @PutMapping("/select-all")
    public Result<Void> selectAll(@RequestBody Map<String, Boolean> body,
                                   Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Boolean selected = body.getOrDefault("selected", true);
        cartService.selectAll(userId, selected);
        return Result.ok(selected ? "已全选" : "已取消全选", null);
    }

    /**
     * 删除单个购物车条目
     *
     * 请求: DELETE /api/v1/cart/5
     */
    @DeleteMapping("/{id}")
    public Result<Void> removeCartItem(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        cartService.removeCartItem(userId, id);
        return Result.ok("已从购物车移除", null);
    }

    /**
     * 清空购物车
     *
     * 请求: DELETE /api/v1/cart
     */
    @DeleteMapping
    public Result<Void> clearCart(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        cartService.clearCart(userId);
        return Result.ok("购物车已清空", null);
    }

    /**
     * 购物车商品种类数（前端角标数字）
     *
     * 请求: GET /api/v1/cart/count
     * 返回: { "code":200, "data": 5 }
     */
    @GetMapping("/count")
    public Result<Long> getCartCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(cartService.getCartCount(userId));
    }
}
