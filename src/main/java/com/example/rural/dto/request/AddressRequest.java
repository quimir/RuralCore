package com.example.rural.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/修改收货地址请求
 *
 * POST /api/v1/addresses      → 新增（所有字段必填）
 * PUT  /api/v1/addresses/{id} → 修改（所有字段必填，全量替换）
 */
@Data
public class AddressRequest {

    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名最多50个字符")
    private String receiverName;

    @NotBlank(message = "收货人手机不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    @Size(max = 50)
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50)
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 50)
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址最多200个字符")
    private String detail;

    /** 是否设为默认地址，可选，默认 false */
    private Boolean isDefault;
}
