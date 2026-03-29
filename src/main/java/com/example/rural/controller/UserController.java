package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.dto.request.ChangePasswordRequest;
import com.example.rural.dto.request.UpdateProfileRequest;
import com.example.rural.dto.response.UserResponse;
import com.example.rural.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人信息控制器（需登录）
 *
 * GET    /api/v1/users/me            获取个人信息
 * PUT    /api/v1/users/me            修改个人资料
 * PUT    /api/v1/users/me/password   修改密码
 *
 * 通过 Authentication.getPrincipal() 获取当前登录用户ID，
 * 确保用户只能操作自己的数据
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的信息
     *
     * 请求: GET /api/v1/users/me
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public Result<UserResponse> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(userService.getUserById(userId));
    }

    /**
     * 修改个人资料
     *
     * 请求: PUT /api/v1/users/me
     * Body: { "nickname": "新昵称", "phone": "13900139000" }
     *
     * 只传需要改的字段，没传的字段不会被覆盖
     */
    @PutMapping("/me")
    public Result<UserResponse> updateMyProfile(Authentication authentication,
                                                @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok("资料修改成功", userService.updateProfile(userId, request));
    }

    /**
     * 修改密码
     *
     * 请求: PUT /api/v1/users/me/password
     * Body: { "oldPassword": "旧密码", "newPassword": "新密码", "confirmPassword": "新密码" }
     */
    @PutMapping("/me/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.changePassword(userId, request);
        return Result.ok("密码修改成功，请重新登录", null);
    }
}
