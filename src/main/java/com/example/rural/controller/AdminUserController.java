package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.AdminUpdateUserRequest;
import com.example.rural.dto.response.UserResponse;
import com.example.rural.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 — 用户管理控制器
 *
 * 所有接口前缀 /api/v1/admin/，SecurityConfig 中配置为 hasRole("ADMIN")
 *
 * GET    /api/v1/admin/users                 分页查询用户列表（支持筛选）
 * GET    /api/v1/admin/users/{id}            查看某用户详情
 * PUT    /api/v1/admin/users/{id}            修改用户信息（角色、状态等）
 * DELETE /api/v1/admin/users/{id}            删除用户
 * PUT    /api/v1/admin/users/{id}/reset-pwd  重置用户密码为默认值
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     *
     * 请求示例:
     *   GET /api/v1/admin/users?keyword=张三&role=FARMER&page=1&size=10
     *
     * 响应:
     * {
     *   "code": 200,
     *   "data": {
     *     "records": [ { "id":1, "username":"farmer01", ... }, ... ],
     *     "total": 50,
     *     "page": 1,
     *     "size": 10,
     *     "totalPages": 5
     *   }
     * }
     */
    @GetMapping
    public Result<PageResult<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userService.listUsers(keyword, role, page, size));
    }

    /** 查看某用户详情 */
    @GetMapping("/{id}")
    public Result<UserResponse> getUserDetail(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    /**
     * 修改用户信息
     *
     * 请求: PUT /api/v1/admin/users/3
     * Body: { "role": "MERCHANT", "status": 0 }
     */
    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody AdminUpdateUserRequest request) {
        return Result.ok("用户信息修改成功", userService.adminUpdateUser(id, request));
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok("用户已删除", null);
    }

    /**
     * 重置用户密码为默认值 (123456)
     *
     * 请求: PUT /api/v1/admin/users/3/reset-pwd
     */
    @PutMapping("/{id}/reset-pwd")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok("密码已重置为默认密码: 123456", null);
    }
}
