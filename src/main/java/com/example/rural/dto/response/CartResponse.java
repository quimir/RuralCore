package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车整体响应
 *
 * 前端购物车页面的完整数据结构：
 *
 * {
 *   "items": [ ... ],          // 所有购物车条目
 *   "totalCount": 5,           // 商品种类数（角标数字）
 *   "selectedCount": 3,        // 已选中数量
 *   "selectedAmount": 49.50,   // 已选中商品总金额（结算按钮显示）
 *   "allSelected": false       // 是否全选（全选按钮状态）
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    /** 购物车所有条目 */
    private List<CartItemResponse> items;

    /** 商品种类数（购物车角标） */
    private Integer totalCount;

    /** 已选中的商品种类数 */
    private Integer selectedCount;

    /** 已选中商品的总金额 */
    private BigDecimal selectedAmount;

    /** 是否全部选中 */
    private Boolean allSelected;
}
