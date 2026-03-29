package com.example.rural.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 投保请求（农户使用）
 */
@Data
public class InsurancePurchaseRequest {

    @NotNull(message = "保险产品ID不能为空")
    private Long productId;

    @NotNull(message = "投保数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;
}
