package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.InsuranceClaimRequest;
import com.example.rural.dto.request.InsuranceProductRequest;
import com.example.rural.dto.request.InsurancePurchaseRequest;
import com.example.rural.entity.InsuranceClaim;
import com.example.rural.entity.InsurancePolicy;
import com.example.rural.entity.InsuranceProduct;
import com.example.rural.entity.User;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.InsuranceClaimRepository;
import com.example.rural.repository.InsurancePolicyRepository;
import com.example.rural.repository.InsuranceProductRepository;
import com.example.rural.repository.UserRepository;
import com.example.rural.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {

    private final InsuranceProductRepository productRepo;
    private final InsurancePolicyRepository policyRepo;
    private final InsuranceClaimRepository claimRepo;
    private final UserRepository userRepository;

    // ==================== 保险产品 ====================

    @Override
    @Transactional
    public Map<String, Object> createInsuranceProduct(Long providerId, String role, InsuranceProductRequest req) {
        checkInsuranceProvider(role);

        InsuranceProduct.Type type;
        try { type = InsuranceProduct.Type.valueOf(req.getType().toUpperCase()); }
        catch (Exception e) { throw new BusinessException("无效的保险类型: " + req.getType()); }

        if (productRepo.existsByProviderIdAndName(providerId, req.getName().trim())) {
            throw new BusinessException("您已有同名保险产品「" + req.getName() + "」");
        }

        InsuranceProduct product = InsuranceProduct.builder()
                .providerId(providerId)
                .name(req.getName().trim())
                .type(type)
                .description(req.getDescription())
                .premium(req.getPremium())
                .premiumUnit(req.getPremiumUnit())
                .coverageAmount(req.getCoverageAmount())
                .coverageMonths(req.getCoverageMonths())
                .coverageScope(req.getCoverageScope())
                .claimConditions(req.getClaimConditions())
                .build();

        InsuranceProduct saved = productRepo.save(product);
        log.info("保险产品发布: id={}, name={}", saved.getId(), saved.getName());
        return toProductMap(saved);
    }

    /**
     * 更新保险产品（支持部分更新）
     * 只有请求中非 null 的字段才会覆盖数据库原值。
     */
    @Override
    @Transactional
    public Map<String, Object> updateInsuranceProduct(Long productId, Long providerId, String role,
                                                       InsuranceProductRequest req) {
        checkInsuranceProvider(role);
        InsuranceProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "保险产品不存在"));
        if (!"ADMIN".equals(role) && !product.getProviderId().equals(providerId)) {
            throw new BusinessException(403, "无权操作");
        }

        // 只更新非 null 字段
        if (req.getName() != null) {
            String newName = req.getName().trim();
            if (!newName.equals(product.getName())
                    && productRepo.existsByProviderIdAndNameAndIdNot(providerId, newName, productId)) {
                throw new BusinessException("您已有同名保险产品「" + newName + "」");
            }
            product.setName(newName);
        }
        if (req.getType() != null) {
            try { product.setType(InsuranceProduct.Type.valueOf(req.getType().toUpperCase())); }
            catch (Exception e) { throw new BusinessException("无效的保险类型: " + req.getType()); }
        }
        if (req.getDescription() != null)    product.setDescription(req.getDescription());
        if (req.getPremium() != null)         product.setPremium(req.getPremium());
        if (req.getPremiumUnit() != null)     product.setPremiumUnit(req.getPremiumUnit());
        if (req.getCoverageAmount() != null)  product.setCoverageAmount(req.getCoverageAmount());
        if (req.getCoverageMonths() != null)  product.setCoverageMonths(req.getCoverageMonths());
        if (req.getCoverageScope() != null)   product.setCoverageScope(req.getCoverageScope());
        if (req.getClaimConditions() != null) product.setClaimConditions(req.getClaimConditions());

        InsuranceProduct saved = productRepo.save(product);
        log.info("保险产品更新: id={}, name={}", productId, saved.getName());
        return toProductMap(saved);
    }

    @Override
    public PageResult<Map<String, Object>> listInsuranceProducts(int page, int size) {
        Page<InsuranceProduct> pageResult = productRepo.findByStatusOrderByCreatedAtDesc(
                InsuranceProduct.Status.ACTIVE, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toProductMap);
    }

    @Override
    public PageResult<Map<String, Object>> listMyInsuranceProducts(Long providerId, int page, int size) {
        Page<InsuranceProduct> pageResult = productRepo.findByProviderIdOrderByCreatedAtDesc(
                providerId, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toProductMap);
    }

    // ==================== 投保 ====================

    @Override
    @Transactional
    public Map<String, Object> purchaseInsurance(Long holderId, String role, InsurancePurchaseRequest req) {
        checkFarmerOrMerchant(role);

        InsuranceProduct product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new BusinessException("保险产品不存在"));
        if (product.getStatus() != InsuranceProduct.Status.ACTIVE) {
            throw new BusinessException("该保险产品暂停销售");
        }

        if (req.getQuantity() <= 0) {
            throw new BusinessException("投保数量必须大于0");
        }

        // 计算保费和保额
        BigDecimal totalPremium = product.getPremium().multiply(BigDecimal.valueOf(req.getQuantity()));
        BigDecimal totalCoverage = product.getCoverageAmount().multiply(BigDecimal.valueOf(req.getQuantity()));

        // 生效日期和到期日期
        LocalDate effectiveDate = req.getEffectiveDate() != null ? req.getEffectiveDate() : LocalDate.now();
        if (effectiveDate.isBefore(LocalDate.now())) {
            throw new BusinessException("生效日期不能是过去");
        }
        LocalDate expiryDate = effectiveDate.plusMonths(product.getCoverageMonths());

        // 生成保单号
        String policyNo = generatePolicyNo();

        InsurancePolicy policy = InsurancePolicy.builder()
                .policyNo(policyNo)
                .holderId(holderId)
                .productId(product.getId())
                .providerId(product.getProviderId())
                .productName(product.getName())
                .quantity(req.getQuantity())
                .quantityUnit(product.getPremiumUnit())
                .totalPremium(totalPremium)
                .totalCoverage(totalCoverage)
                .effectiveDate(effectiveDate)
                .expiryDate(expiryDate)
                .build();

        InsurancePolicy saved = policyRepo.save(policy);
        log.info("保单创建: policyNo={}, product={}, premium={}", policyNo, product.getName(), totalPremium);
        return toPolicyMap(saved);
    }

    @Override
    @Transactional
    public void payPolicy(Long policyId, Long holderId) {
        InsurancePolicy policy = policyRepo.findById(policyId)
                .orElseThrow(() -> new BusinessException(404, "保单不存在"));
        if (!policy.getHolderId().equals(holderId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (policy.getStatus() != InsurancePolicy.Status.PENDING_PAYMENT) {
            throw new BusinessException("当前状态不允许支付");
        }
        policy.setStatus(InsurancePolicy.Status.ACTIVE);
        policy.setPaidAt(LocalDateTime.now());
        policyRepo.save(policy);
        log.info("保单已支付: policyNo={}", policy.getPolicyNo());
    }

    @Override
    public PageResult<Map<String, Object>> listMyPolicies(Long holderId, int page, int size) {
        Page<InsurancePolicy> pageResult = policyRepo.findByHolderIdOrderByCreatedAtDesc(
                holderId, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toPolicyMap);
    }

    // ==================== 理赔 ====================

    @Override
    @Transactional
    public Map<String, Object> fileClaim(Long claimantId, String role, InsuranceClaimRequest req) {
        checkFarmerOrMerchant(role);

        InsurancePolicy policy = policyRepo.findById(req.getPolicyId())
                .orElseThrow(() -> new BusinessException("保单不存在"));
        if (!policy.getHolderId().equals(claimantId)) {
            throw new BusinessException(403, "此保单不属于您");
        }
        if (policy.getStatus() != InsurancePolicy.Status.ACTIVE) {
            throw new BusinessException("保单非生效状态，无法理赔");
        }

        // 出险日期必须在保障期内
        if (req.getIncidentDate().isBefore(policy.getEffectiveDate())
                || req.getIncidentDate().isAfter(policy.getExpiryDate())) {
            throw new BusinessException("出险日期不在保障期内（"
                    + policy.getEffectiveDate() + " ~ " + policy.getExpiryDate() + "）");
        }

        // 索赔金额不能超过保额
        if (req.getClaimAmount().compareTo(policy.getTotalCoverage()) > 0) {
            throw new BusinessException("索赔金额不能超过保额（" + policy.getTotalCoverage() + "元）");
        }

        // 检查是否已有进行中的理赔
        Set<InsuranceClaim.Status> activeStatuses = Set.of(
                InsuranceClaim.Status.SUBMITTED, InsuranceClaim.Status.UNDER_REVIEW);
        if (claimRepo.existsByPolicyIdAndStatusIn(req.getPolicyId(), activeStatuses)) {
            throw new BusinessException("该保单已有进行中的理赔申请");
        }

        InsuranceClaim claim = InsuranceClaim.builder()
                .policyId(policy.getId())
                .claimantId(claimantId)
                .providerId(policy.getProviderId())
                .incidentDate(req.getIncidentDate())
                .incidentDesc(req.getIncidentDesc())
                .claimAmount(req.getClaimAmount())
                .evidenceImages(req.getEvidenceImages())
                .build();

        InsuranceClaim saved = claimRepo.save(claim);
        log.info("理赔报案: id={}, policyNo={}", saved.getId(), policy.getPolicyNo());
        return toClaimMap(saved);
    }

    @Override
    public PageResult<Map<String, Object>> listMyClaims(Long claimantId, int page, int size) {
        Page<InsuranceClaim> pageResult = claimRepo.findByClaimantIdOrderByCreatedAtDesc(
                claimantId, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toClaimMap);
    }

    @Override
    public PageResult<Map<String, Object>> listClaimsForProvider(Long providerId, String status,
                                                                   int page, int size) {
        Page<InsuranceClaim> pageResult;
        if (status != null && !status.isBlank()) {
            try {
                InsuranceClaim.Status s = InsuranceClaim.Status.valueOf(status.toUpperCase());
                pageResult = claimRepo.findByProviderIdAndStatusOrderByCreatedAtDesc(
                        providerId, s, PageRequest.of(page - 1, size));
            } catch (Exception e) { throw new BusinessException("无效的状态: " + status); }
        } else {
            pageResult = claimRepo.findByProviderIdOrderByCreatedAtDesc(
                    providerId, PageRequest.of(page - 1, size));
        }
        return PageResult.from(pageResult, this::toClaimMap);
    }

    @Override
    @Transactional
    public Map<String, Object> reviewClaim(Long claimId, Long reviewerId, String role,
                                            boolean approved, String comment,
                                            BigDecimal assessedAmount, BigDecimal payoutAmount) {
        checkInsuranceProvider(role);

        InsuranceClaim claim = claimRepo.findById(claimId)
                .orElseThrow(() -> new BusinessException(404, "理赔单不存在"));
        if (!"ADMIN".equals(role) && !claim.getProviderId().equals(reviewerId)) {
            throw new BusinessException(403, "此理赔单不属于您的机构");
        }
        if (claim.getStatus() != InsuranceClaim.Status.SUBMITTED
                && claim.getStatus() != InsuranceClaim.Status.UNDER_REVIEW) {
            throw new BusinessException("当前状态不允许审核");
        }

        if (approved) {
            if (assessedAmount == null || payoutAmount == null) {
                throw new BusinessException("批准时必须填写定损金额和赔付金额");
            }
            claim.setStatus(InsuranceClaim.Status.APPROVED);
            claim.setAssessedAmount(assessedAmount);
            claim.setPayoutAmount(payoutAmount);

            // 赔付后保单状态变为已理赔
            InsurancePolicy policy = policyRepo.findById(claim.getPolicyId()).orElse(null);
            if (policy != null) {
                policy.setStatus(InsurancePolicy.Status.CLAIMED);
                policyRepo.save(policy);
            }
        } else {
            if (comment == null || comment.isBlank()) {
                throw new BusinessException("拒绝时必须填写原因");
            }
            claim.setStatus(InsuranceClaim.Status.REJECTED);
        }

        claim.setReviewComment(comment);
        claim.setReviewerId(reviewerId);
        claim.setReviewedAt(LocalDateTime.now());
        InsuranceClaim saved = claimRepo.save(claim);

        log.info("理赔审核: id={}, result={}", claimId, approved ? "批准" : "拒绝");
        return toClaimMap(saved);
    }

    // ==================== 工具方法 ====================

    private void checkInsuranceProvider(String role) {
        if (!"INSURANCE_PROVIDER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "仅保险提供商或管理员可执行此操作");
        }
    }

    private void checkFarmerOrMerchant(String role) {
        if (!"FARMER".equals(role) && !"MERCHANT".equals(role)) {
            throw new BusinessException(403, "仅农户或商家可投保/理赔");
        }
    }

    private String generatePolicyNo() {
        return "POL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%06d", ThreadLocalRandom.current().nextInt(999999));
    }

    private Map<String, Object> toProductMap(InsuranceProduct p) {
        User provider = userRepository.findById(p.getProviderId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getName());
        map.put("type", p.getType().name());
        map.put("description", p.getDescription());
        map.put("premium", p.getPremium());
        map.put("premiumUnit", p.getPremiumUnit());
        map.put("coverageAmount", p.getCoverageAmount());
        map.put("coverageMonths", p.getCoverageMonths());
        map.put("coverageScope", p.getCoverageScope());
        map.put("claimConditions", p.getClaimConditions());
        map.put("status", p.getStatus().name());
        map.put("providerName", provider != null ? provider.getOrgName() : null);
        map.put("createdAt", p.getCreatedAt());
        return map;
    }

    private Map<String, Object> toPolicyMap(InsurancePolicy p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("policyNo", p.getPolicyNo());
        map.put("productName", p.getProductName());
        map.put("quantity", p.getQuantity());
        map.put("quantityUnit", p.getQuantityUnit());
        map.put("totalPremium", p.getTotalPremium());
        map.put("totalCoverage", p.getTotalCoverage());
        map.put("effectiveDate", p.getEffectiveDate());
        map.put("expiryDate", p.getExpiryDate());
        map.put("status", p.getStatus().name());
        map.put("paidAt", p.getPaidAt());
        map.put("createdAt", p.getCreatedAt());
        return map;
    }

    private Map<String, Object> toClaimMap(InsuranceClaim c) {
        InsurancePolicy policy = policyRepo.findById(c.getPolicyId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("policyNo", policy != null ? policy.getPolicyNo() : null);
        map.put("productName", policy != null ? policy.getProductName() : null);
        map.put("incidentDate", c.getIncidentDate());
        map.put("incidentDesc", c.getIncidentDesc());
        map.put("claimAmount", c.getClaimAmount());
        map.put("evidenceImages", c.getEvidenceImages());
        map.put("status", c.getStatus().name());
        map.put("assessedAmount", c.getAssessedAmount());
        map.put("payoutAmount", c.getPayoutAmount());
        map.put("reviewComment", c.getReviewComment());
        map.put("reviewedAt", c.getReviewedAt());
        map.put("createdAt", c.getCreatedAt());
        return map;
    }
}
