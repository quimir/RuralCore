package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细实体（订单行）
 *
 * 数据库表: order_item
 *
 * 为什么要冗余存储 productName / productImage / unitPrice？
 *   下单时的价格和商品信息是「快照」，即使商家后来改了商品名或调了价，
 *   已下单的历史记录不应该变。这是电商系统的标准做法。
 *
 * 关联关系：
 *   order_item.order_id   → orders.id   （所属订单）
 *   order_item.product_id → product.id  （关联商品，用于跳转详情页）
 *   order_item.seller_id  → user.id     （卖家，用于卖家查询「我卖出的」）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_item", indexes = {
        @Index(name = "idx_oi_order", columnList = "order_id"),
        @Index(name = "idx_oi_seller", columnList = "seller_id")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属订单ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 商品ID */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 卖家ID（冗余，方便卖家查「我卖出的订单」） */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // ========== 下单时的商品快照 ==========

    /** 商品名称（快照） */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /** 商品图片（快照） */
    @Column(name = "product_image", length = 500)
    private String productImage;

    /** 下单时单价（快照） */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** 购买数量 */
    @Column(nullable = false)
    private Integer quantity;

    /** 计量单位（快照） */
    @Column(length = 20)
    private String unit;

    /** 小计 = unitPrice × quantity */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
