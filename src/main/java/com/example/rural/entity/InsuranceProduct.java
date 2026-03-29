package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农业保险产品实体
 *
 * 数据库表: insurance_product
 *
 * 保险提供商发布保险产品供农户投保:
 *
 *   水稻种植险:
 *     保费: 18元/亩    保额: 800元/亩   保障期: 一个种植季
 *     保障范围: 洪涝、干旱、病虫害导致的减产
 *
 *   生猪养殖险:
 *     保费: 60元/头    保额: 1500元/头  保障期: 12个月
 *     保障范围: 疫病死亡
 *
 * 关联关系:
 *   insurance_product.provider_id → user.id (INSURANCE_PROVIDER)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "insurance_product", indexes = {
        @Index(name = "idx_ip_provider", columnList = "provider_id"),
        @Index(name = "idx_ip_type", columnList = "type")
})
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发布者（保险提供商） */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false, length = 100)
    private String name;

    /** 保险类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 保费单价（元/亩 或 元/头 或 元/份） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal premium;

    /** 保费计量单位 */
    @Column(name = "premium_unit", nullable = false, length = 20)
    private String premiumUnit;

    /** 保额单价（元/亩 或 元/头 或 元/份） */
    @Column(name = "coverage_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal coverageAmount;

    /** 保障期限（月） */
    @Column(name = "coverage_months", nullable = false)
    private Integer coverageMonths;

    /** 保障范围说明 */
    @Column(name = "coverage_scope", length = 1000)
    private String coverageScope;

    /** 理赔条件说明 */
    @Column(name = "claim_conditions", length = 1000)
    private String claimConditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Type {
        CROP,       // 种植险
        LIVESTOCK,  // 养殖险
        FORESTRY,   // 林木险
        FACILITY,   // 设施农业险（大棚/温室）
        INCOME      // 收入保险（价格波动保障）
    }

    public enum Status {
        ACTIVE,    // 销售中
        SUSPENDED, // 暂停
        OFFLINE    // 已下线
    }
}
