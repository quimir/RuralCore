package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.InsuranceClaimRequest;
import com.example.rural.dto.request.InsuranceProductRequest;
import com.example.rural.dto.request.InsurancePurchaseRequest;
import com.example.rural.service.InsuranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 保险服务控制器
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  【保险产品 — 保险提供商 INSURANCE_PROVIDER】                             │
 * │  POST   /api/v1/finance/insurance-products              发布保险产品     │
 * │  PUT    /api/v1/finance/insurance-products/{id}          编辑保险产品     │
 * │  GET    /api/v1/finance/insurance-products/mine          我发布的产品     │
 * │                                                                          │
 * │  【浏览保险产品 — 农户/商家】                                             │
 * │  GET    /api/v1/finance/insurance-products               浏览可投保的     │
 * │                                                                          │
 * │  【投保 — 农户/商家】                                                     │
 * │  POST   /api/v1/finance/policies                         投保             │
 * │  PUT    /api/v1/finance/policies/{id}/pay                支付保费         │
 * │  GET    /api/v1/finance/policies/mine                    我的保单列表     │
 * │                                                                          │
 * │  【理赔 — 农户报案 + 保险商审核】                                         │
 * │  POST   /api/v1/finance/claims                           报案             │
 * │  GET    /api/v1/finance/claims/mine                      我的理赔列表     │
 * │  GET    /api/v1/finance/claims/review                    待审核理赔列表   │
 * │  PUT    /api/v1/finance/claims/{id}/review               审核理赔         │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    // ==================== 保险产品 ====================

    /** 浏览保险产品（ACTIVE） */
    @GetMapping("/insurance-products")
    public Result<PageResult<Map<String, Object>>> listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(insuranceService.listInsuranceProducts(page, size));
    }

    /** 发布保险产品 */
    @PostMapping("/insurance-products")
    public Result<Map<String, Object>> createProduct(@Valid @RequestBody InsuranceProductRequest request,
                                                      Authentication auth) {
        return Result.ok("保险产品发布成功",
                insuranceService.createInsuranceProduct(userId(auth), role(auth), request));
    }

    /** 编辑保险产品（PUT 全量 / PATCH 部分，Service 层只更新非 null 字段） */
    @PutMapping("/insurance-products/{id}")
    public Result<Map<String, Object>> updateProduct(@PathVariable Long id,
                                                      @RequestBody InsuranceProductRequest request,
                                                      Authentication auth) {
        return Result.ok(insuranceService.updateInsuranceProduct(id, userId(auth), role(auth), request));
    }

    /** 部分修改保险产品（只传要改的字段） */
    @PatchMapping("/insurance-products/{id}")
    public Result<Map<String, Object>> patchProduct(@PathVariable Long id,
                                                     @RequestBody InsuranceProductRequest request,
                                                     Authentication auth) {
        return Result.ok(insuranceService.updateInsuranceProduct(id, userId(auth), role(auth), request));
    }

    /** 我发布的保险产品 */
    @GetMapping("/insurance-products/mine")
    public Result<PageResult<Map<String, Object>>> listMyProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(insuranceService.listMyInsuranceProducts(userId(auth), page, size));
    }

    // ==================== 投保 ====================

    /** 农户投保 */
    @PostMapping("/policies")
    public Result<Map<String, Object>> purchase(@Valid @RequestBody InsurancePurchaseRequest request,
                                                 Authentication auth) {
        return Result.ok("投保成功", insuranceService.purchaseInsurance(userId(auth), role(auth), request));
    }

    /** 支付保费 */
    @PutMapping("/policies/{id}/pay")
    public Result<Void> payPolicy(@PathVariable Long id, Authentication auth) {
        insuranceService.payPolicy(id, userId(auth));
        return Result.ok("保费支付成功", null);
    }

    /** 我的保单 */
    @GetMapping("/policies/mine")
    public Result<PageResult<Map<String, Object>>> listMyPolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(insuranceService.listMyPolicies(userId(auth), page, size));
    }

    // ==================== 理赔 ====================

    /** 报案 */
    @PostMapping("/claims")
    public Result<Map<String, Object>> fileClaim(@Valid @RequestBody InsuranceClaimRequest request,
                                                  Authentication auth) {
        return Result.ok("理赔报案成功", insuranceService.fileClaim(userId(auth), role(auth), request));
    }

    /** 我的理赔列表 */
    @GetMapping("/claims/mine")
    public Result<PageResult<Map<String, Object>>> listMyClaims(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(insuranceService.listMyClaims(userId(auth), page, size));
    }

    /** 保险商查看待审核理赔 */
    @GetMapping("/claims/review")
    public Result<PageResult<Map<String, Object>>> listClaimsForReview(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return Result.ok(insuranceService.listClaimsForProvider(userId(auth), status, page, size));
    }

    /** 审核理赔 */
    @PutMapping("/claims/{id}/review")
    public Result<Map<String, Object>> reviewClaim(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> body,
                                                    Authentication auth) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = (String) body.get("comment");
        BigDecimal assessed = body.get("assessedAmount") != null
                ? new BigDecimal(body.get("assessedAmount").toString()) : null;
        BigDecimal payout = body.get("payoutAmount") != null
                ? new BigDecimal(body.get("payoutAmount").toString()) : null;

        return Result.ok(approved ? "已批准赔付" : "已拒绝",
                insuranceService.reviewClaim(id, userId(auth), role(auth),
                        approved, comment, assessed, payout));
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
