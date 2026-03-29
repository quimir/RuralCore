package com.example.rural.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 贷款申请请求（农户使用）
 */
@Data
public class LoanApplyRequest {

    @NotNull(message = "贷款产品ID不能为空")
    private Long productId;

    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal applyAmount;

    @NotNull(message = "申请期限不能为空")
    @Min(value = 1, message = "期限至少1个月")
    private Integer applyTermMonths;

    @NotBlank(message = "贷款用途不能为空")
    private String purpose;
}
