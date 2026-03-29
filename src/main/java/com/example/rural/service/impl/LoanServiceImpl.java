package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.LoanApplyRequest;
import com.example.rural.dto.request.LoanProductRequest;
import com.example.rural.dto.response.CreditReportResponse;
import com.example.rural.entity.LoanApplication;
import com.example.rural.entity.LoanProduct;
import com.example.rural.entity.User;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.LoanApplicationRepository;
import com.example.rural.repository.LoanProductRepository;
import com.example.rural.repository.UserRepository;
import com.example.rural.service.FinancialService;
import com.example.rural.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanProductRepository productRepo;
    private final LoanApplicationRepository appRepo;
    private final UserRepository userRepository;
    private final FinancialService financialService;

    // ==================== 贷款产品 ====================

    @Override
    @Transactional
    public Map<String, Object> createLoanProduct(Long providerId, String role, LoanProductRequest req) {
        checkFinancialProvider(role);

        LoanProduct.Type type;
        try { type = LoanProduct.Type.valueOf(req.getType().toUpperCase()); }
        catch (Exception e) { throw new BusinessException("无效的贷款类型: " + req.getType()); }

        if (productRepo.existsByProviderIdAndName(providerId, req.getName().trim())) {
            throw new BusinessException("您已有同名贷款产品「" + req.getName() + "」");
        }

        LoanProduct product = LoanProduct.builder()
                .providerId(providerId)
                .name(req.getName().trim())
                .type(type)
                .description(req.getDescription())
                .minAmount(req.getMinAmount())
                .maxAmount(req.getMaxAmount())
                .annualRate(req.getAnnualRate())
                .minTermMonths(req.getMinTermMonths())
                .maxTermMonths(req.getMaxTermMonths())
                .minCreditScore(req.getMinCreditScore() != null ? req.getMinCreditScore() : 0)
                .requirements(req.getRequirements())
                .repaymentMethod(req.getRepaymentMethod())
                .build();

        LoanProduct saved = productRepo.save(product);
        log.info("贷款产品发布: id={}, name={}", saved.getId(), saved.getName());
        return toLoanProductMap(saved);
    }

    /**
     * 更新贷款产品（支持部分更新）
     *
     * 核心逻辑: 只有请求中非 null 的字段才会覆盖数据库原值。
     *
     * 全量更新（PUT）:  前端传所有字段，全部覆盖
     * 部分更新（PATCH）: 前端只传要改的字段，其余保持不变
     *
     * 举例:
     *   数据库: { name: "春耕贷", minCreditScore: 40, annualRate: 4.35 }
     *   请求:   { minCreditScore: 35 }
     *   结果:   { name: "春耕贷", minCreditScore: 35, annualRate: 4.35 }
     *                                  ↑ 只有这个变了
     */
    @Override
    @Transactional
    public Map<String, Object> updateLoanProduct(Long productId, Long providerId, String role,
                                                  LoanProductRequest req) {
        checkFinancialProvider(role);
        LoanProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "贷款产品不存在"));
        if (!"ADMIN".equals(role) && !product.getProviderId().equals(providerId)) {
            throw new BusinessException(403, "无权操作");
        }

        // 只更新非 null 字段（支持 PATCH 部分更新）
        if (req.getName() != null) {
            String newName = req.getName().trim();
            // 改了名字才检查重名（排除自己）
            if (!newName.equals(product.getName())
                    && productRepo.existsByProviderIdAndNameAndIdNot(providerId, newName, productId)) {
                throw new BusinessException("您已有同名贷款产品「" + newName + "」");
            }
            product.setName(newName);
        }
        if (req.getType() != null) {
            try { product.setType(LoanProduct.Type.valueOf(req.getType().toUpperCase())); }
            catch (Exception e) { throw new BusinessException("无效的贷款类型: " + req.getType()); }
        }
        if (req.getDescription() != null)    product.setDescription(req.getDescription());
        if (req.getMinAmount() != null)       product.setMinAmount(req.getMinAmount());
        if (req.getMaxAmount() != null)       product.setMaxAmount(req.getMaxAmount());
        if (req.getAnnualRate() != null)      product.setAnnualRate(req.getAnnualRate());
        if (req.getMinTermMonths() != null)   product.setMinTermMonths(req.getMinTermMonths());
        if (req.getMaxTermMonths() != null)   product.setMaxTermMonths(req.getMaxTermMonths());
        if (req.getMinCreditScore() != null)  product.setMinCreditScore(req.getMinCreditScore());
        if (req.getRequirements() != null)    product.setRequirements(req.getRequirements());
        if (req.getRepaymentMethod() != null) product.setRepaymentMethod(req.getRepaymentMethod());

        LoanProduct saved = productRepo.save(product);
        log.info("贷款产品更新: id={}, name={}", productId, saved.getName());
        return toLoanProductMap(saved);
    }

    @Override
    @Transactional
    public void offlineLoanProduct(Long productId, Long providerId, String role) {
        checkFinancialProvider(role);
        LoanProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "贷款产品不存在"));
        if (!"ADMIN".equals(role) && !product.getProviderId().equals(providerId)) {
            throw new BusinessException(403, "无权操作");
        }
        product.setStatus(LoanProduct.Status.OFFLINE);
        productRepo.save(product);
    }

    @Override
    public PageResult<Map<String, Object>> listLoanProducts(int page, int size) {
        Page<LoanProduct> pageResult = productRepo.findByStatusOrderByCreatedAtDesc(
                LoanProduct.Status.ACTIVE, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toLoanProductMap);
    }

    @Override
    public PageResult<Map<String, Object>> listMyLoanProducts(Long providerId, int page, int size) {
        Page<LoanProduct> pageResult = productRepo.findByProviderIdOrderByCreatedAtDesc(
                providerId, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toLoanProductMap);
    }

    // ==================== 贷款申请 ====================

    @Override
    @Transactional
    public Map<String, Object> applyLoan(Long applicantId, String role, LoanApplyRequest req) {
        checkFarmerOrMerchant(role);

        LoanProduct product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new BusinessException("贷款产品不存在"));
        if (product.getStatus() != LoanProduct.Status.ACTIVE) {
            throw new BusinessException("该贷款产品暂不接受申请");
        }

        // 金额范围校验
        if (req.getApplyAmount().compareTo(product.getMinAmount()) < 0
                || req.getApplyAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("申请金额需在 " + product.getMinAmount() + " ~ "
                    + product.getMaxAmount() + " 范围内");
        }

        // 期限范围校验
        if (req.getApplyTermMonths() < product.getMinTermMonths()
                || req.getApplyTermMonths() > product.getMaxTermMonths()) {
            throw new BusinessException("申请期限需在 " + product.getMinTermMonths() + " ~ "
                    + product.getMaxTermMonths() + " 个月范围内");
        }

        // 检查是否有进行中的申请
        Set<LoanApplication.Status> activeStatuses = Set.of(
                LoanApplication.Status.SUBMITTED, LoanApplication.Status.UNDER_REVIEW);
        if (appRepo.existsByApplicantIdAndProductIdAndStatusIn(applicantId, req.getProductId(), activeStatuses)) {
            throw new BusinessException("您已有该产品的进行中申请，请等待审核结果");
        }

        // 获取信用评分
        CreditReportResponse credit = financialService.getCreditReport(applicantId, applicantId, role);

        // 信用评分门槛检查
        if (credit.getTotalScore() < product.getMinCreditScore()) {
            throw new BusinessException("您的信用评分（" + credit.getTotalScore()
                    + "）未达到该产品要求（≥" + product.getMinCreditScore() + "），暂无法申请");
        }

        LoanApplication app = LoanApplication.builder()
                .applicantId(applicantId)
                .productId(product.getId())
                .providerId(product.getProviderId())
                .applyAmount(req.getApplyAmount())
                .applyTermMonths(req.getApplyTermMonths())
                .purpose(req.getPurpose())
                .creditScoreSnapshot(credit.getTotalScore())
                .annualRevenueSnapshot(credit.getTotalTransactionAmount())
                .build();

        LoanApplication saved = appRepo.save(app);
        log.info("贷款申请提交: id={}, product={}, amount={}", saved.getId(), product.getName(), req.getApplyAmount());
        return toApplicationMap(saved);
    }

    @Override
    public PageResult<Map<String, Object>> listMyApplications(Long applicantId, int page, int size) {
        Page<LoanApplication> pageResult = appRepo.findByApplicantIdOrderByCreatedAtDesc(
                applicantId, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toApplicationMap);
    }

    @Override
    public PageResult<Map<String, Object>> listApplicationsForProvider(Long providerId, String status,
                                                                        int page, int size) {
        Page<LoanApplication> pageResult;
        if (status != null && !status.isBlank()) {
            try {
                LoanApplication.Status s = LoanApplication.Status.valueOf(status.toUpperCase());
                pageResult = appRepo.findByProviderIdAndStatusOrderByCreatedAtDesc(
                        providerId, s, PageRequest.of(page - 1, size));
            } catch (Exception e) { throw new BusinessException("无效的状态: " + status); }
        } else {
            pageResult = appRepo.findByProviderIdOrderByCreatedAtDesc(
                    providerId, PageRequest.of(page - 1, size));
        }
        return PageResult.from(pageResult, this::toApplicationMap);
    }

    @Override
    @Transactional
    public Map<String, Object> reviewApplication(Long applicationId, Long reviewerId, String role,
                                                  boolean approved, String comment,
                                                  BigDecimal approvedAmount, BigDecimal approvedRate) {
        checkFinancialProvider(role);
        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请不存在"));
        if (!"ADMIN".equals(role) && !app.getProviderId().equals(reviewerId)) {
            throw new BusinessException(403, "此申请不属于您的机构");
        }
        if (app.getStatus() != LoanApplication.Status.SUBMITTED
                && app.getStatus() != LoanApplication.Status.UNDER_REVIEW) {
            throw new BusinessException("当前状态不允许审核");
        }

        if (approved) {
            if (approvedAmount == null || approvedRate == null) {
                throw new BusinessException("批准时必须填写批准金额和利率");
            }
            app.setStatus(LoanApplication.Status.APPROVED);
            app.setApprovedAmount(approvedAmount);
            app.setApprovedRate(approvedRate);
        } else {
            if (comment == null || comment.isBlank()) {
                throw new BusinessException("拒绝时必须填写原因");
            }
            app.setStatus(LoanApplication.Status.REJECTED);
        }
        app.setReviewComment(comment);
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        LoanApplication saved = appRepo.save(app);
        log.info("贷款审核: id={}, result={}", applicationId, approved ? "批准" : "拒绝");
        return toApplicationMap(saved);
    }

    // ==================== 工具方法 ====================

    private void checkFinancialProvider(String role) {
        if (!"FINANCIAL_PROVIDER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "仅金融服务商或管理员可执行此操作");
        }
    }

    private void checkFarmerOrMerchant(String role) {
        if (!"FARMER".equals(role) && !"MERCHANT".equals(role)) {
            throw new BusinessException(403, "仅农户或商家可申请贷款");
        }
    }

    private Map<String, Object> toLoanProductMap(LoanProduct p) {
        User provider = userRepository.findById(p.getProviderId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getName());
        map.put("type", p.getType().name());
        map.put("description", p.getDescription());
        map.put("minAmount", p.getMinAmount());
        map.put("maxAmount", p.getMaxAmount());
        map.put("annualRate", p.getAnnualRate());
        map.put("minTermMonths", p.getMinTermMonths());
        map.put("maxTermMonths", p.getMaxTermMonths());
        map.put("minCreditScore", p.getMinCreditScore());
        map.put("requirements", p.getRequirements());
        map.put("repaymentMethod", p.getRepaymentMethod());
        map.put("status", p.getStatus().name());
        map.put("providerName", provider != null ? provider.getOrgName() : null);
        map.put("createdAt", p.getCreatedAt());
        return map;
    }

    private Map<String, Object> toApplicationMap(LoanApplication a) {
        LoanProduct product = productRepo.findById(a.getProductId()).orElse(null);
        User applicant = userRepository.findById(a.getApplicantId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("productName", product != null ? product.getName() : null);
        map.put("productType", product != null ? product.getType().name() : null);
        map.put("applicantName", applicant != null ? applicant.getNickname() : null);
        map.put("applyAmount", a.getApplyAmount());
        map.put("applyTermMonths", a.getApplyTermMonths());
        map.put("purpose", a.getPurpose());
        map.put("creditScoreSnapshot", a.getCreditScoreSnapshot());
        map.put("annualRevenueSnapshot", a.getAnnualRevenueSnapshot());
        map.put("status", a.getStatus().name());
        map.put("reviewComment", a.getReviewComment());
        map.put("approvedAmount", a.getApprovedAmount());
        map.put("approvedRate", a.getApprovedRate());
        map.put("reviewedAt", a.getReviewedAt());
        map.put("createdAt", a.getCreatedAt());
        return map;
    }
}
