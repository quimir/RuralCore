package com.example.rural.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求
 *
 * 所有字段都是可选的，只传需要修改的字段
 * 前端示例: PUT /api/v1/users/me  { "nickname": "新昵称", "phone": "13900139000" }
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 头像URL */
    @Size(max = 255)
    private String avatar;
}
