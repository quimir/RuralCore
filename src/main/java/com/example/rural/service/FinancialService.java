package com.example.rural.service;

import com.example.rural.dto.response.CreditReportResponse;
import com.example.rural.dto.response.FinancialDashboardResponse;

/**
 * 财务分析服务
 *
 * 核心功能:
 *   1. 财务看板: 收入概览 + 月度趋势 + 信用 + 建议
 *   2. 信用报告: 从订单/产品/保单数据自动评分
 *   3. 经营建议: 基于数据规则生成建议
 *
 * 数据来源:
 *   收入数据 ← OrderItem.sellerId + Order.status=COMPLETED
 *   商品数据 ← Product.sellerId + Product.status=ON_SALE
 *   保障数据 ← InsurancePolicy.holderId + status=ACTIVE
 *   注册时长 ← User.createdAt
 */
public interface FinancialService {

    /**
     * 获取农户/商家的财务看板
     * @param farmerId 农户/商家ID
     * @param requesterId 请求者ID（用于权限校验）
     * @param role 请求者角色
     */
    FinancialDashboardResponse getDashboard(Long farmerId, Long requesterId, String role);

    /**
     * 获取信用报告
     * @param farmerId 被评估的农户/商家ID
     * @param requesterId 请求者ID
     * @param role 请求者角色
     */
    CreditReportResponse getCreditReport(Long farmerId, Long requesterId, String role);
}
