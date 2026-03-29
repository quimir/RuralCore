package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车条目实体
 *
 * 数据库表: cart_item
 *
 * 设计思路：
 *   为什么存数据库而不是 Redis / 前端 localStorage？
 *   1. 用户换设备登录后购物车还在（云端持久化）
 *   2. 不会因为清缓存/Redis重启丢失
 *   3. 可以做后续的「购物车商品降价提醒」等功能
 *   4. 数据量不大（每个用户几十条），MySQL 完全够用
 *
 * 唯一约束: (user_id, product_id)
 *   同一用户对同一商品只有一条记录，重复加入则累加数量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_product",
                columnNames = {"user_id", "product_id"}
        ),
        indexes = @Index(name = "idx_cart_user", columnList = "user_id")
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 商品ID */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 购买数量 */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 是否选中（用于结算时部分下单）
     *
     * 前端购物车页面通常有勾选框，
     * 用户可以只勾选部分商品去结算，
     * 这个字段记录勾选状态，也存云端。
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean selected = true;

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
}
