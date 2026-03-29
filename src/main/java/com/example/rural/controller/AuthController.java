package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.dto.request.LoginRequest;
import com.example.rural.dto.request.RegisterRequest;
import com.example.rural.dto.response.LoginResponse;
import com.example.rural.dto.response.UserResponse;
import com.example.rural.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（公开接口，无需登录）
 *
 * POST /api/v1/auth/register  注册
 * POST /api/v1/auth/login     登录
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", userService.login(request));
    }
}
