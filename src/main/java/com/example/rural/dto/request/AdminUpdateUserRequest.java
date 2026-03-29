package com.example.rural.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员修改用户信息请求
 *
 * 管理员可修改: 昵称、手机、邮箱、角色、账号状态
 * 不可修改: 用户名（唯一标识）、密码（需用户自行修改）
 */
@Data
public class AdminUpdateUserRequest {

    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 角色: ADMIN / FARMER / MERCHANT / TOURIST */
    private String role;

    /** 账号状态: 0-禁用 1-正常 */
    private Integer status;
}
