package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款产品实体
 *
 * 数据库表: loan_product
 *
 * 金融服务商发布贷款产品供农户/商家申请:
 *
 *   春耕贷:
 *     额度: 1万-20万    年利率: 4.35%   期限: 3-12个月
 *     用途: 种子、化肥、农药等春耕生产资料
 *     要求: 信用评分≥60
 *
 *   设备购置贷:
 *     额度: 5万-100万   年利率: 5.60%   期限: 12-60个月
 *     用途: 农机具、加工设备、冷链设施
 *     要求: 信用评分≥70，需有6个月以上经营记录
 *
 * 关联关系:
 *   loan_product.provider_id → user.id (FINANCIAL_PROVIDER)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_product", indexes = {
        @Index(name = "idx_lp_provider", columnList = "provider_id"),
        @Index(name = "idx_lp_status", columnList = "status")
})
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发布者（金融服务商） */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false, length = 100)
    private String name;

    /** 贷款类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 最低额度 */
    @Column(name = "min_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal minAmount;

    /** 最高额度 */
    @Column(name = "max_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal maxAmount;

    /** 年利率（%），如 4.35 表示 4.35% */
    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRate;

    /** 最短期限（月） */
    @Column(name = "min_term_months", nullable = false)
    private Integer minTermMonths;

    /** 最长期限（月） */
    @Column(name = "max_term_months", nullable = false)
    private Integer maxTermMonths;

    /** 所需最低信用评分 */
    @Column(name = "min_credit_score")
    @Builder.Default
    private Integer minCreditScore = 0;

    /** 申请条件说明 */
    @Column(name = "requirements", length = 1000)
    private String requirements;

    /** 还款方式说明 */
    @Column(name = "repayment_method", length = 200)
    private String repaymentMethod;

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
        CROP_LOAN,       // 种植贷（春耕贷、秋收贷）
        EQUIPMENT_LOAN,  // 设备购置贷
        WORKING_CAPITAL, // 流动资金贷
        LAND_LOAN,       // 土地流转贷
        AGRI_CHAIN       // 农业产业链贷
    }

    public enum Status {
        ACTIVE,    // 上架中
        SUSPENDED, // 暂停申请
        OFFLINE    // 已下线
    }
}
