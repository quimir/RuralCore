package com.example.rural.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加购物车请求
 *
 * 前端调用: POST /api/v1/cart
 * Body: { "productId": 1, "quantity": 2 }
 *
 * 如果购物车里已经有这个商品，后端会自动累加数量（不会重复创建）
 */
@Data
public class CartAddRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少为1")
    private Integer quantity;
}
