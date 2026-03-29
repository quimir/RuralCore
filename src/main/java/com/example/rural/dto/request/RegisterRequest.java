package com.example.rural.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数
 *
 * 使用 Jakarta Validation 注解自动校验，
 * 校验失败会被 GlobalExceptionHandler 捕获并返回友好提示
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    private String password;

    /** 确认密码（后端二次校验） */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 昵称（可选） */
    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    /** 手机号（可选） */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（可选） */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 注册角色（可选，默认 TOURIST）
     * 可选值: FARMER, MERCHANT, TOURIST, FINANCIAL_PROVIDER, INSURANCE_PROVIDER
     * ADMIN 不允许通过注册接口创建
     */
    private String role;

    /** 机构名称（金融服务商/保险提供商必填） */
    private String orgName;

    /** 经营许可编号（金融服务商/保险提供商必填） */
    private String licenseNo;
}
