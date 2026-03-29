package com.example.rural.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 贷款产品请求（金融服务商使用）
 */
@Data
public class LoanProductRequest {

    @NotBlank(message = "产品名称不能为空")
    private String name;

    @NotNull(message = "贷款类型不能为空")
    private String type;

    private String description;

    @NotNull(message = "最低额度不能为空")
    @DecimalMin(value = "0.01", message = "最低额度必须大于0")
    private BigDecimal minAmount;

    @NotNull(message = "最高额度不能为空")
    private BigDecimal maxAmount;

    @NotNull(message = "年利率不能为空")
    @DecimalMin(value = "0.01")
    private BigDecimal annualRate;

    @NotNull @Min(1)
    private Integer minTermMonths;

    @NotNull @Min(1)
    private Integer maxTermMonths;

    private Integer minCreditScore;

    private String requirements;

    private String repaymentMethod;
}
