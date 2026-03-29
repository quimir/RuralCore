package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.RouteCreateRequest;
import com.example.rural.dto.response.RouteResponse;
import com.example.rural.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 旅游路线控制器
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  接口一览                                                       │
 * │                                                                 │
 * │  【公开】                                                       │
 * │  GET  /api/v1/tourism/routes/public        推荐路线（热门）      │
 * │  GET  /api/v1/tourism/routes/{id}          路线详情              │
 * │                                                                 │
 * │  【需登录】                                                     │
 * │  POST /api/v1/tourism/routes               创建路线              │
 * │  PUT  /api/v1/tourism/routes/{id}          编辑路线              │
 * │  DELETE /api/v1/tourism/routes/{id}        删除路线              │
 * │  GET  /api/v1/tourism/routes/mine          我的路线              │
 * │  POST /api/v1/tourism/routes/{id}/copy     复制公开路线          │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * 使用场景:
 *   1. 用户在景点详情页点「加入行程」→ 选择已有路线或创建新路线
 *   2. 在「我的路线」中拖拽排序、分天、设时间
 *   3. 设为公开 → 其他用户可以看到和复制
 *   4. 热门路线按引用次数排序展示
 */
@RestController
@RequestMapping("/api/v1/tourism/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // ==================== 公开 ====================

    /** 推荐路线（公开的，按热度排序） */
    @GetMapping("/public")
    public Result<PageResult<RouteResponse>> getPublicRoutes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(routeService.getPublicRoutes(page, size));
    }

    /** 路线详情（公开路线所有人可看，私有路线仅创建者） */
    @GetMapping("/{id}")
    public Result<RouteResponse> getRouteDetail(@PathVariable Long id, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof Long
                ? (Long) auth.getPrincipal() : null;
        return Result.ok(routeService.getRouteDetail(id, userId));
    }

    // ==================== 需登录 ====================

    /** 创建路线 */
    @PostMapping
    public Result<RouteResponse> createRoute(@Valid @RequestBody RouteCreateRequest request,
                                              Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("路线创建成功", routeService.createRoute(userId, request));
    }

    /** 编辑路线（整体替换行程项） */
    @PutMapping("/{id}")
    public Result<RouteResponse> updateRoute(@PathVariable Long id,
                                              @Valid @RequestBody RouteCreateRequest request,
                                              Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(routeService.updateRoute(id, userId, request));
    }

    /** 删除路线 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRoute(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        routeService.deleteRoute(id, userId);
        return Result.ok("路线已删除", null);
    }

    /** 我的路线 */
    @GetMapping("/mine")
    public Result<PageResult<RouteResponse>> getMyRoutes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(routeService.getMyRoutes(userId, page, size));
    }

    /** 复制公开路线到我的名下 */
    @PostMapping("/{id}/copy")
    public Result<RouteResponse> copyRoute(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("已复制到我的路线", routeService.copyRoute(id, userId));
    }
}
