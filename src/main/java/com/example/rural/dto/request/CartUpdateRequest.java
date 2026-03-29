package com.example.rural.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 更新购物车条目请求
 *
 * 前端调用: PUT /api/v1/cart/{id}
 * Body: { "quantity": 3 }          → 修改数量
 *   或: { "selected": false }      → 取消勾选
 *   或: { "quantity": 5, "selected": true }  → 同时修改
 *
 * 两个字段都是可选的，只传需要改的
 */
@Data
public class CartUpdateRequest {

    @Min(value = 1, message = "数量最少为1")
    private Integer quantity;

    private Boolean selected;
}
