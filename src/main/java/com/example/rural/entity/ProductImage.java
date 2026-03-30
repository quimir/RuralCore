package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农产品详情图片实体
 *
 * 一个产品可以有多张详情图片（封面图仍然在 Product.imageUrl 中）。
 * 商户可以上传多张图片来展示产品的详细信息，如不同角度、包装、产地实拍等。
 *
 * 关联关系：
 *   product_image.product_id → product.id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_image", indexes = {
        @Index(name = "idx_pi_product", columnList = "product_id")
})
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属产品ID */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 图片URL（可以是网络路径或本地缓存路径） */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /** 图片说明/描述（可选） */
    @Column(length = 200)
    private String caption;

    /** 排序序号（越小越靠前） */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 图片类型
     * DETAIL  — 详情图（展示在产品详情页）
     * SPEC    — 规格图（展示产品规格参数）
     * ORIGIN  — 产地实拍
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 20)
    @Builder.Default
    private ImageType imageType = ImageType.DETAIL;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ImageType {
        DETAIL,   // 详情图
        SPEC,     // 规格图
        ORIGIN    // 产地实拍
    }
}
