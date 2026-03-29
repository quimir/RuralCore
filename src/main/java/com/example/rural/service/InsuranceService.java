package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.InsuranceClaimRequest;
import com.example.rural.dto.request.InsuranceProductRequest;
import com.example.rural.dto.request.InsurancePurchaseRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface InsuranceService {

    // ========== 保险产品（保险提供商） ==========
    Map<String, Object> createInsuranceProduct(Long providerId, String role, InsuranceProductRequest request);
    Map<String, Object> updateInsuranceProduct(Long productId, Long providerId, String role, InsuranceProductRequest request);
    PageResult<Map<String, Object>> listInsuranceProducts(int page, int size);
    PageResult<Map<String, Object>> listMyInsuranceProducts(Long providerId, int page, int size);

    // ========== 投保（农户） ==========
    Map<String, Object> purchaseInsurance(Long holderId, String role, InsurancePurchaseRequest request);
    void payPolicy(Long policyId, Long holderId);
    PageResult<Map<String, Object>> listMyPolicies(Long holderId, int page, int size);

    // ========== 理赔 ==========
    Map<String, Object> fileClaim(Long claimantId, String role, InsuranceClaimRequest request);
    PageResult<Map<String, Object>> listMyClaims(Long claimantId, int page, int size);
    PageResult<Map<String, Object>> listClaimsForProvider(Long providerId, String status, int page, int size);
    Map<String, Object> reviewClaim(Long claimId, Long reviewerId, String role,
                                     boolean approved, String comment,
                                     BigDecimal assessedAmount, BigDecimal payoutAmount);
}
