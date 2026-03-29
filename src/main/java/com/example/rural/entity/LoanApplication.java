package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款申请实体
 *
 * 数据库表: loan_application
 *
 * 申请流程:
 *   农户选贷款产品 → 填写申请（金额/期限/用途）→ 系统附上信用报告
 *   → 金融服务商审核 → 批准(放款) / 拒绝(给原因)
 *
 * 状态流转:
 *   SUBMITTED → UNDER_REVIEW → APPROVED → DISBURSED → REPAID
 *                    │
 *                    └→ REJECTED
 *
 * 关联关系:
 *   loan_application.applicant_id  → user.id (FARMER/MERCHANT)
 *   loan_application.product_id    → loan_product.id
 *   loan_application.provider_id   → user.id (FINANCIAL_PROVIDER，冗余便于查询)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_application", indexes = {
        @Index(name = "idx_la_applicant", columnList = "applicant_id"),
        @Index(name = "idx_la_provider", columnList = "provider_id"),
        @Index(name = "idx_la_status", columnList = "status")
})
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请人（农户/商家） */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /** 贷款产品 */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 金融服务商（冗余） */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    // ========== 申请信息 ==========

    /** 申请金额 */
    @Column(name = "apply_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal applyAmount;

    /** 申请期限（月） */
    @Column(name = "apply_term_months", nullable = false)
    private Integer applyTermMonths;

    /** 贷款用途详述 */
    @Column(nullable = false, length = 1000)
    private String purpose;

    /** 申请时的信用评分（快照） */
    @Column(name = "credit_score_snapshot")
    private Integer creditScoreSnapshot;

    /** 申请时的年收入（快照） */
    @Column(name = "annual_revenue_snapshot", precision = 14, scale = 2)
    private BigDecimal annualRevenueSnapshot;

    // ========== 审批信息 ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SUBMITTED;

    /** 审批意见 */
    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    /** 批准金额（可能和申请金额不同） */
    @Column(name = "approved_amount", precision = 14, scale = 2)
    private BigDecimal approvedAmount;

    /** 批准利率 */
    @Column(name = "approved_rate", precision = 5, scale = 2)
    private BigDecimal approvedRate;

    /** 审核人ID */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Status {
        SUBMITTED,     // 已提交
        UNDER_REVIEW,  // 审核中
        APPROVED,      // 已批准
        REJECTED,      // 已拒绝
        DISBURSED,     // 已放款
        REPAID         // 已还清
    }
}
