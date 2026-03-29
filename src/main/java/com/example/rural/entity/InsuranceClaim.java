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
 * 保险理赔实体
 *
 * 数据库表: insurance_claim
 *
 * 理赔流程:
 *   农户报案 → 提交损失说明/证据 → 保险商定损审核 → 赔付
 *
 *   SUBMITTED → UNDER_REVIEW → APPROVED → PAID_OUT
 *                    │
 *                    └→ REJECTED
 *
 * 关联关系:
 *   insurance_claim.policy_id   → insurance_policy.id
 *   insurance_claim.claimant_id → user.id (FARMER)
 *   insurance_claim.provider_id → user.id (INSURANCE_PROVIDER)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "insurance_claim", indexes = {
        @Index(name = "idx_ic_policy", columnList = "policy_id"),
        @Index(name = "idx_ic_claimant", columnList = "claimant_id"),
        @Index(name = "idx_ic_provider", columnList = "provider_id")
})
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "claimant_id", nullable = false)
    private Long claimantId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    // ========== 报案信息 ==========

    /** 出险日期 */
    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    /** 损失类型描述（如 "暴雨导致水稻倒伏"） */
    @Column(name = "incident_desc", nullable = false, length = 1000)
    private String incidentDesc;

    /** 索赔金额 */
    @Column(name = "claim_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal claimAmount;

    /** 证据材料（图片URL，JSON数组） */
    @Column(name = "evidence_images", length = 2000)
    private String evidenceImages;

    // ========== 审核信息 ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SUBMITTED;

    /** 定损金额（保险商评估的实际损失） */
    @Column(name = "assessed_amount", precision = 14, scale = 2)
    private BigDecimal assessedAmount;

    /** 赔付金额 */
    @Column(name = "payout_amount", precision = 14, scale = 2)
    private BigDecimal payoutAmount;

    /** 审核意见 */
    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "paid_out_at")
    private LocalDateTime paidOutAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Status {
        SUBMITTED,    // 已报案
        UNDER_REVIEW, // 定损审核中
        APPROVED,     // 已批准
        REJECTED,     // 已拒绝
        PAID_OUT      // 已赔付
    }
}
