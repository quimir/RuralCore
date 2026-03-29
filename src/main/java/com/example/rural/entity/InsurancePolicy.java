package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保险保单实体
 *
 * 数据库表: insurance_policy
 *
 * 农户购买保险后生成保单:
 *   保单号: POL-20260315-A3K7M2
 *   产品: 水稻种植险
 *   数量: 50亩   保费: 900元（18×50）   保额: 40000元（800×50）
 *   有效期: 2026-03-15 ~ 2026-09-15
 *
 * 状态:
 *   PENDING_PAYMENT → ACTIVE → EXPIRED / CLAIMED
 *                       │
 *                       └→ CANCELLED
 *
 * 关联关系:
 *   insurance_policy.holder_id   → user.id (FARMER/MERCHANT)
 *   insurance_policy.product_id  → insurance_product.id
 *   insurance_policy.provider_id → user.id (INSURANCE_PROVIDER)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "insurance_policy", indexes = {
        @Index(name = "idx_pol_holder", columnList = "holder_id"),
        @Index(name = "idx_pol_provider", columnList = "provider_id"),
        @Index(name = "idx_pol_no", columnList = "policy_no")
})
public class InsurancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 保单号 */
    @Column(name = "policy_no", nullable = false, unique = true, length = 30)
    private String policyNo;

    /** 投保人（农户/商家） */
    @Column(name = "holder_id", nullable = false)
    private Long holderId;

    /** 保险产品 */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 保险提供商（冗余） */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    // ========== 快照 ==========

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /** 投保数量（亩/头/份） */
    @Column(nullable = false)
    private Integer quantity;

    /** 计量单位 */
    @Column(name = "quantity_unit", length = 20)
    private String quantityUnit;

    /** 总保费 */
    @Column(name = "total_premium", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPremium;

    /** 总保额 */
    @Column(name = "total_coverage", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCoverage;

    /** 生效日期 */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /** 到期日期 */
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING_PAYMENT;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Status {
        PENDING_PAYMENT, // 待付款
        ACTIVE,          // 生效中
        EXPIRED,         // 已过期
        CLAIMED,         // 已理赔
        CANCELLED        // 已退保
    }
}
