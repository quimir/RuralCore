package com.example.rural.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 理赔报案请求（农户使用）
 */
@Data
public class InsuranceClaimRequest {

    @NotNull(message = "保单ID不能为空")
    private Long policyId;

    @NotNull(message = "出险日期不能为空")
    private LocalDate incidentDate;

    @NotBlank(message = "损失描述不能为空")
    private String incidentDesc;

    @NotNull(message = "索赔金额不能为空")
    @DecimalMin(value = "0.01")
    private BigDecimal claimAmount;

    private String evidenceImages;
}
