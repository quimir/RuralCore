package com.example.rural.service;

import com.example.rural.dto.request.CartAddRequest;
import com.example.rural.dto.request.CartUpdateRequest;
import com.example.rural.dto.response.CartItemResponse;
import com.example.rural.dto.response.CartResponse;

/**
 * 购物车服务接口
 *
 * 数据存储在 MySQL cart_item 表，实现云端持久化。
 *
 * 核心逻辑：
 *   1. 加入购物车: 已有则累加数量，没有则新建
 *   2. 查看购物车: 实时查商品表获取最新价格和库存
 *   3. 修改: 改数量、改选中状态
 *   4. 全选/取消全选: 批量操作
 *   5. 删除: 单个删除或批量删除已选中的
 */
public interface CartService {

    /** 添加商品到购物车（已存在则累加数量） */
    CartItemResponse addToCart(Long userId, CartAddRequest request);

    /** 查看我的购物车（含汇总信息） */
    CartResponse getMyCart(Long userId);

    /** 修改购物车条目（数量/选中状态） */
    CartItemResponse updateCartItem(Long userId, Long cartItemId, CartUpdateRequest request);

    /** 全选 / 取消全选 */
    void selectAll(Long userId, boolean selected);

    /** 删除单个购物车条目 */
    void removeCartItem(Long userId, Long cartItemId);

    /** 清空购物车 */
    void clearCart(Long userId);

    /** 购物车商品数量（前端角标用） */
    long getCartCount(Long userId);
}
