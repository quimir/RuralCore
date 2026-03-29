package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.dto.response.CreditReportResponse;
import com.example.rural.dto.response.FinancialDashboardResponse;
import com.example.rural.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * 财务分析控制器
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  【财务看板 — 农户/商家自己查自己，管理员查任何人】                         │
 * │  GET  /api/v1/finance/dashboard                 我的财务看板              │
 * │  GET  /api/v1/finance/dashboard/{farmerId}      查看指定农户（管理员）     │
 * │                                                                          │
 * │  【信用报告 — 农户自己、管理员、有关联的金融服务商】                        │
 * │  GET  /api/v1/finance/credit-report             我的信用报告              │
 * │  GET  /api/v1/finance/credit-report/{farmerId}  查看指定农户              │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * 财务看板包含:
 *   📊 收入概览（总收入、本月、环比增长）
 *   📈 12个月收入趋势图
 *   ⭐ 信用评分 (经营历史30分 + 交易信用40分 + 风险评估30分)
 *   🛡️ 保障概览（生效保单数、总保额）
 *   💡 AI经营建议（基于规则引擎自动生成）
 *
 * 隐私保护:
 *   金融服务商不能直接看农户财务看板（只有在贷款申请流程中才能看信用报告）
 *   保险提供商不能看任何财务数据
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    /** 我的财务看板 */
    @GetMapping("/dashboard")
    public Result<FinancialDashboardResponse> getMyDashboard(Authentication auth) {
        Long userId = userId(auth);
        String role = role(auth);
        return Result.ok(financialService.getDashboard(userId, userId, role));
    }

    /** 管理员查看指定农户的财务看板 */
    @GetMapping("/dashboard/{farmerId}")
    public Result<FinancialDashboardResponse> getDashboard(@PathVariable Long farmerId,
                                                            Authentication auth) {
        return Result.ok(financialService.getDashboard(farmerId, userId(auth), role(auth)));
    }

    /** 我的信用报告 */
    @GetMapping("/credit-report")
    public Result<CreditReportResponse> getMyCreditReport(Authentication auth) {
        Long userId = userId(auth);
        String role = role(auth);
        return Result.ok(financialService.getCreditReport(userId, userId, role));
    }

    /** 查看指定农户的信用报告（管理员/有关联的金融服务商） */
    @GetMapping("/credit-report/{farmerId}")
    public Result<CreditReportResponse> getCreditReport(@PathVariable Long farmerId,
                                                         Authentication auth) {
        return Result.ok(financialService.getCreditReport(farmerId, userId(auth), role(auth)));
    }

    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    private String role(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_") && !"ROLE_USER".equals(a))
                .map(a -> a.substring(5))
                .findFirst().orElse("TOURIST");
    }
}
