package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.*;
import com.example.rural.dto.response.LoginResponse;
import com.example.rural.dto.response.UserResponse;

/**
 * 用户服务接口
 *
 * 三大职责：
 * 1. 认证（注册/登录）
 * 2. 个人信息管理（修改资料/修改密码）
 * 3. 管理员用户管理（查看列表/修改角色/禁用账号/删除）
 */
public interface UserService {

    // ==================== 认证 ====================

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    // ==================== 个人信息 ====================

    /** 获取用户信息 */
    UserResponse getUserById(Long userId);

    /** 修改个人资料（昵称、手机、邮箱、头像） */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordRequest request);

    // ==================== 管理员操作 ====================

    /** 分页查询用户列表（支持按用户名/角色筛选） */
    PageResult<UserResponse> listUsers(String keyword, String role, int page, int size);

    /** 管理员修改用户信息（角色、状态等） */
    UserResponse adminUpdateUser(Long userId, AdminUpdateUserRequest request);

    /** 管理员删除用户 */
    void deleteUser(Long userId);

    /** 管理员重置用户密码 */
    void resetPassword(Long userId);
}
