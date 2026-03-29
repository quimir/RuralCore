package com.example.rural.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保险产品请求（保险提供商使用）
 */
@Data
public class InsuranceProductRequest {

    @NotBlank(message = "产品名称不能为空")
    private String name;

    @NotNull(message = "保险类型不能为空")
    private String type;

    private String description;

    @NotNull(message = "保费不能为空")
    @DecimalMin(value = "0.01")
    private BigDecimal premium;

    @NotBlank(message = "保费单位不能为空")
    private String premiumUnit;  // "元/亩", "元/头", "元/份"

    @NotNull(message = "保额不能为空")
    private BigDecimal coverageAmount;

    @NotNull(message = "保障期限不能为空")
    @Min(1)
    private Integer coverageMonths;

    private String coverageScope;

    private String claimConditions;
}
