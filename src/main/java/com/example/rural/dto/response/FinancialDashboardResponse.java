package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务看板响应
 *
 * 农户/商家的「我的财务」页面:
 *
 *   ┌───────────────────────────────────────────────┐
 *   │  💰 总收入: ¥126,800    📦 已完成: 234单       │
 *   │  📊 本月: ¥12,500       🔄 进行中: 8单         │
 *   │                                               │
 *   │  📈 月度收入趋势                               │
 *   │  ┌──────────────────────────────┐             │
 *   │  │   ▂ ▄ █ ▆ ▃ ▇ █ ▅ ▃ ▆ ▇ █  │             │
 *   │  │  1  2  3  4  5  6  7  8  9..│             │
 *   │  └──────────────────────────────┘             │
 *   │                                               │
 *   │  ⭐ 信用评分: 82 / 100    等级: 良好            │
 *   │                                               │
 *   │  💡 经营建议:                                   │
 *   │  · 您的苹果类产品销量持续增长，建议扩大库存      │
 *   │  · 订单完成率98%，信用评分优秀                   │
 *   │  · 本月收入同比增长23%，可考虑申请设备贷扩产     │
 *   └───────────────────────────────────────────────┘
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDashboardResponse {

    // ========== 收入概览 ==========

    /** 历史总收入 */
    private BigDecimal totalRevenue;
    /** 本月收入 */
    private BigDecimal monthRevenue;
    /** 上月收入（用于计算环比） */
    private BigDecimal lastMonthRevenue;
    /** 收入环比增长率（%，可为负） */
    private BigDecimal revenueGrowthRate;
    /** 总完成订单数 */
    private Long totalCompletedOrders;
    /** 进行中订单数 */
    private Long activeOrders;
    /** 在售商品数 */
    private Long activeProducts;

    // ========== 月度趋势（最近12个月） ==========

    private List<MonthlyRevenue> monthlyTrend;

    // ========== 信用评分 ==========

    private CreditReportResponse creditReport;

    // ========== 保障概览 ==========

    /** 生效中的保单数 */
    private Long activePolicies;
    /** 总保障额度 */
    private BigDecimal totalCoverage;

    // ========== AI 经营建议 ==========

    private List<String> suggestions;

    /**
     * 月度收入数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;          // "2026-01"
        private BigDecimal revenue;
        private Long orderCount;
    }
}
