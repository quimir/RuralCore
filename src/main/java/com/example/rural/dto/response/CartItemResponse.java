package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车条目响应
 *
 * 前端购物车页面需要展示：
 *   - 商品图片、名称、单价、单位（从 product 表实时读取）
 *   - 购买数量、是否选中（从 cart_item 表读取）
 *   - 小计金额（后端计算: price × quantity）
 *   - 库存是否充足（前端据此显示 "库存不足" 提示）
 *
 *  ┌────────────────────────────────────────────────┐
 *  │  [✓]  [商品图]  红富士苹果         ×3          │
 *  │                 ¥5.50/斤    小计: ¥16.50       │
 *  │                 库存: 500                       │
 *  └────────────────────────────────────────────────┘
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    /** 购物车条目ID（用于更新和删除） */
    private Long id;

    /** 商品ID（用于跳转详情页） */
    private Long productId;

    // ---- 商品实时信息（从 product 表查询）----
    private String productName;
    private String productImage;
    private BigDecimal price;
    private String unit;
    private Integer stock;
    private String productStatus;   // ON_SALE / OFF_SHELF / SOLD_OUT

    // ---- 购物车信息 ----
    private Integer quantity;
    private Boolean selected;

    /** 小计 = price × quantity */
    private BigDecimal subtotal;

    /** 是否可购买（在售 且 库存 >= 数量） */
    private Boolean available;

    private LocalDateTime createdAt;
}
