package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.LoanApplyRequest;
import com.example.rural.dto.request.LoanProductRequest;
import com.example.rural.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 贷款服务控制器
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  【贷款产品 — 金融服务商 FINANCIAL_PROVIDER】                         │
 * │  POST   /api/v1/finance/loan-products                 发布贷款产品   │
 * │  PUT    /api/v1/finance/loan-products/{id}             编辑贷款产品   │
 * │  DELETE /api/v1/finance/loan-products/{id}             下线贷款产品   │
 * │  GET    /api/v1/finance/loan-products/mine             我发布的产品   │
 * │                                                                      │
 * │  【浏览贷款产品 — 农户/商家 FARMER/MERCHANT】                         │
 * │  GET    /api/v1/finance/loan-products                  浏览可申请的   │
 * │                                                                      │
 * │  【贷款申请 — 农户/商家】                                             │
 * │  POST   /api/v1/finance/loan-applications              申请贷款       │
 * │  GET    /api/v1/finance/loan-applications/mine          我的申请列表   │
 * │                                                                      │
 * │  【贷款审核 — 金融服务商】                                            │
 * │  GET    /api/v1/finance/loan-applications/review        待审核列表     │
 * │  PUT    /api/v1/finance/loan-applications/{id}/review   审核          │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // ==================== 贷款产品 ====================

    /** 浏览贷款产品（农户可见的 ACTIVE 产品） */
    @GetMapping("/loan-products")
    public Result<PageResult<Map<String, Object>>> listLoanProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(loanService.listLoanProducts(page, size));
    }

    /** 金融服务商发布贷款产品 */
    @PostMapping("/loan-products")
    public Result<Map<String, Object>> createLoanProduct(@Valid @RequestBody LoanProductRequest request,
                                                          Authentication auth) {
        return Result.ok("贷款产品发布成功", loanService.createLoanProduct(
                userId(auth), role(auth), request));
    }

    /**
     * 全量编辑贷款产品（PUT 语义: 必须传所有字段）
     *
     * 注意: 不加 @Valid，允许部分字段为 null 时仍然走到 Service 层，
     * Service 层只更新非 null 字段。如果想严格全量替换，前端把所有字段都传上来即可。
     */
    @PutMapping("/loan-products/{id}")
    public Result<Map<String, Object>> updateLoanProduct(@PathVariable Long id,
                                                          @RequestBody LoanProductRequest request,
                                                          Authentication auth) {
        return Result.ok(loanService.updateLoanProduct(id, userId(auth), role(auth), request));
    }

    /**
     * 部分修改贷款产品（PATCH 语义: 只传要改的字段）
     *
     * 使用场景:
     *   只改信用门槛:  { "minCreditScore": 35 }
     *   只改利率:      { "annualRate": 3.85 }
     *   改额度范围:    { "minAmount": 5000, "maxAmount": 300000 }
     */
    @PatchMapping("/loan-products/{id}")
    public Result<Map<String, Object>> patchLoanProduct(@PathVariable Long id,
                                                         @RequestBody LoanProductRequest request,
                                                         Authentication auth) {
        return Result.ok(loanService.updateLoanProduct(id, userId(auth), role(auth), request));
    }

    /** 下线贷款产品 */
    @DeleteMapping("/loan-products/{id}")
    public Result<Void> offlineLoanProduct(@PathVariable Long id, Authentication auth) {
        loanService.offlineLoanProduct(id, userId(auth), role(auth));
        return Result.ok("已下线", null);
    }

    /** 我发布的贷款产品 */
    @GetMapping("/loan-products/mine")
    public Result<PageResult<Map<String, Object>>> listMyLoanProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(loanService.listMyLoanProducts(userId(auth), page, size));
    }

    // ==================== 贷款申请 ====================

    /** 农户申请贷款 */
    @PostMapping("/loan-applications")
    public Result<Map<String, Object>> applyLoan(@Valid @RequestBody LoanApplyRequest request,
                                                  Authentication auth) {
        return Result.ok("贷款申请已提交", loanService.applyLoan(userId(auth), role(auth), request));
    }

    /** 我的贷款申请 */
    @GetMapping("/loan-applications/mine")
    public Result<PageResult<Map<String, Object>>> listMyApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(loanService.listMyApplications(userId(auth), page, size));
    }

    /** 金融服务商查看待审核列表 */
    @GetMapping("/loan-applications/review")
    public Result<PageResult<Map<String, Object>>> listApplicationsForReview(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(loanService.listApplicationsForProvider(userId(auth), status, page, size));
    }

    /** 审核贷款申请 */
    @PutMapping("/loan-applications/{id}/review")
    public Result<Map<String, Object>> reviewApplication(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> body,
                                                          Authentication auth) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = (String) body.get("comment");
        BigDecimal approvedAmount = body.get("approvedAmount") != null
                ? new BigDecimal(body.get("approvedAmount").toString()) : null;
        BigDecimal approvedRate = body.get("approvedRate") != null
                ? new BigDecimal(body.get("approvedRate").toString()) : null;

        return Result.ok(approved ? "已批准" : "已拒绝",
                loanService.reviewApplication(id, userId(auth), role(auth),
                        approved, comment, approvedAmount, approvedRate));
    }

    // ==================== 工具 ====================

    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    private String role(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_") && !"ROLE_USER".equals(a))
                .map(a -> a.substring(5))
                .findFirst().orElse("TOURIST");
    }
}
