package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.LoanApplyRequest;
import com.example.rural.dto.request.LoanProductRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface LoanService {

    // ========== 贷款产品（金融服务商） ==========

    Map<String, Object> createLoanProduct(Long providerId, String role, LoanProductRequest request);

    Map<String, Object> updateLoanProduct(Long productId, Long providerId, String role, LoanProductRequest request);

    void offlineLoanProduct(Long productId, Long providerId, String role);

    /** 浏览贷款产品（农户可见 ACTIVE 的） */
    PageResult<Map<String, Object>> listLoanProducts(int page, int size);

    /** 金融服务商查看自己的产品 */
    PageResult<Map<String, Object>> listMyLoanProducts(Long providerId, int page, int size);

    // ========== 贷款申请（农户） ==========

    Map<String, Object> applyLoan(Long applicantId, String role, LoanApplyRequest request);

    /** 我的贷款申请 */
    PageResult<Map<String, Object>> listMyApplications(Long applicantId, int page, int size);

    /** 金融服务商审核列表 */
    PageResult<Map<String, Object>> listApplicationsForProvider(Long providerId, String status, int page, int size);

    /** 审核贷款申请 */
    Map<String, Object> reviewApplication(Long applicationId, Long reviewerId, String role,
                                           boolean approved, String comment,
                                           BigDecimal approvedAmount, BigDecimal approvedRate);
}
