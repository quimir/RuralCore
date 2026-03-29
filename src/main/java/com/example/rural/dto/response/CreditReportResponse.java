package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信用报告响应
 *
 * 评分模型（满分100）:
 *
 *   经营历史（30分）:
 *     注册时长 ≥ 12个月: +10, ≥ 6个月: +5
 *     在售商品数 ≥ 10: +10, ≥ 5: +5
 *     经营活跃度: +10（近30天有订单完成）
 *
 *   交易信用（40分）:
 *     订单完成率 ≥ 95%: +20, ≥ 80%: +10
 *     累计成交额 ≥ 10万: +10, ≥ 1万: +5
 *     无取消订单: +10
 *
 *   风险评估（30分）:
 *     收入趋势上升: +10
 *     有保险保障: +10
 *     无逾期贷款: +10
 *
 * 等级: 优秀(≥80) / 良好(≥60) / 一般(≥40) / 较差(<40)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditReportResponse {

    /** 总分（0-100） */
    private Integer totalScore;

    /** 等级: EXCELLENT / GOOD / FAIR / POOR */
    private String grade;

    // ========== 评分明细 ==========

    /** 经营历史得分（满分30） */
    private Integer historyScore;

    /** 交易信用得分（满分40） */
    private Integer tradeScore;

    /** 风险评估得分（满分30） */
    private Integer riskScore;

    // ========== 关键指标 ==========

    /** 注册天数 */
    private Long registeredDays;

    /** 在售商品数 */
    private Long activeProductCount;

    /** 累计完成订单数 */
    private Long completedOrderCount;

    /** 订单完成率（%） */
    private BigDecimal completionRate;

    /** 累计成交额 */
    private BigDecimal totalTransactionAmount;

    /** 近30天成交额 */
    private BigDecimal recentMonthRevenue;

    /** 生效保单数 */
    private Long activePolicyCount;

    /** 生成时间 */
    private LocalDateTime generatedAt;
}
