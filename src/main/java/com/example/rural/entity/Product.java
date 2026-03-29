package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农产品实体
 *
 * 关联关系：
 *   product.category_id → product_category.id  （所属分类）
 *   product.seller_id   → user.id              （发布者/商家）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_category", columnList = "category_id"),
        @Index(name = "idx_seller", columnList = "seller_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 产品名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 产品描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 单价 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 库存 */
    @Builder.Default
    private Integer stock = 0;

    /** 计量单位（斤/公斤/箱/个） */
    @Column(length = 20)
    private String unit;

    /** 产地 */
    @Column(length = 100)
    private String origin;

    /** 所属分类ID */
    @Column(name = "category_id")
    private Long categoryId;

    /** 发布者（商家/农户）ID */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /** 主图URL */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 商品状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ON_SALE;

    /** 销量（按件数累计） */
    @Column(name = "sales_count")
    @Builder.Default
    private Integer salesCount = 0;

    /**
     * 购买人次（每笔订单包含此商品时 +1，取消订单时 -1）
     * 注意: 这是订单次数，不是去重后的人数
     */
    @Column(name = "buyer_count")
    @Builder.Default
    private Integer buyerCount = 0;

    /**
     * 商品标签（逗号分隔）
     *
     * 示例: "有机,绿色食品,助农,时令"
     *
     * 设计决策: 用逗号分隔字符串而不是独立关联表。
     * 理由:
     *   - 农产品标签量少（一般 1~5 个）
     *   - 读远多于写，单字段 LIKE 查询足够
     *   - 省去 product_tag 关联表的 JOIN 开销
     *   - 前端传入传出都方便（直接一个字符串）
     */
    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        ON_SALE,    // 在售
        OFF_SHELF,  // 已下架
        SOLD_OUT    // 售罄
    }
}
