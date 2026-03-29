package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.TourismReviewRequest;
import com.example.rural.dto.request.TourismSpotQueryRequest;
import com.example.rural.dto.request.TourismSpotRequest;
import com.example.rural.dto.response.TourismReviewResponse;
import com.example.rural.dto.response.TourismSpotResponse;
import com.example.rural.service.TourismService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 旅游推广控制器
 *
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  接口一览                                                              │
 * │                                                                        │
 * │  【公开 - 游客可访问】                                                  │
 * │  GET  /api/v1/tourism/spots              景点列表（筛选+排序+分页）      │
 * │  GET  /api/v1/tourism/spots/{id}         景点详情（浏览量+1）           │
 * │  GET  /api/v1/tourism/spots/{id}/reviews 景点评论列表                   │
 * │                                                                        │
 * │  【需登录 - 商家/农户/管理员】                                           │
 * │  POST /api/v1/tourism/spots              发布旅游项目                   │
 * │  PUT  /api/v1/tourism/spots/{id}         编辑旅游项目                   │
 * │  DELETE /api/v1/tourism/spots/{id}       删除旅游项目                   │
 * │  GET  /api/v1/tourism/spots/mine         我发布的旅游项目               │
 * │                                                                        │
 * │  【需登录 - 所有用户】                                                  │
 * │  POST /api/v1/tourism/spots/{id}/reviews 发表评论                       │
 * │  DELETE /api/v1/tourism/reviews/{id}     删除评论                       │
 * │  POST /api/v1/tourism/spots/{id}/favorite 收藏/取消收藏                  │
 * │  GET  /api/v1/tourism/favorites          我的收藏列表                   │
 * └────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/tourism")
@RequiredArgsConstructor
public class TourismController {

    private final TourismService tourismService;

    // ==================== 公开接口（游客可访问） ====================

    /**
     * 景点列表
     *
     * GET /api/v1/tourism/spots?keyword=采摘&type=PICKING_GARDEN&sort=rating_desc&page=1&size=10
     *
     * 类型: SCENIC_SPOT / FARMSTAY / PICKING_GARDEN / AGRI_EXPERIENCE / FOLK_CULTURE
     * 排序: newest / rating_desc / views_desc / price_asc / price_desc
     */
    @GetMapping("/spots")
    public Result<PageResult<TourismSpotResponse>> listSpots(TourismSpotQueryRequest query,
                                                              Authentication auth) {
        Long currentUserId = extractUserId(auth);
        return Result.ok(tourismService.listSpots(query, currentUserId));
    }

    /**
     * 景点详情（浏览量自动+1）
     */
    @GetMapping("/spots/{id}")
    public Result<TourismSpotResponse> getSpotDetail(@PathVariable Long id,
                                                      Authentication auth) {
        Long currentUserId = extractUserId(auth);
        return Result.ok(tourismService.getSpotDetail(id, currentUserId));
    }

    /**
     * 景点评论列表
     */
    @GetMapping("/spots/{spotId}/reviews")
    public Result<PageResult<TourismReviewResponse>> getSpotReviews(
            @PathVariable Long spotId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(tourismService.getSpotReviews(spotId, page, size));
    }

    // ==================== 需登录 — 发布/编辑/删除 ====================

    /**
     * 发布旅游项目
     *
     * 商家/农户 → 待审核（PENDING）
     * 管理员   → 直接上线（APPROVED）
     * 游客     → 拒绝（403）
     */
    @PostMapping("/spots")
    public Result<TourismSpotResponse> createSpot(@Valid @RequestBody TourismSpotRequest request,
                                                   Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return Result.ok("发布成功", tourismService.createSpot(userId, role, request));
    }

    /**
     * 编辑旅游项目（发布者本人或管理员）
     */
    @PutMapping("/spots/{id}")
    public Result<TourismSpotResponse> updateSpot(@PathVariable Long id,
                                                   @Valid @RequestBody TourismSpotRequest request,
                                                   Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return Result.ok("修改成功", tourismService.updateSpot(id, userId, role, request));
    }

    /**
     * 删除旅游项目（发布者本人或管理员）
     */
    @DeleteMapping("/spots/{id}")
    public Result<Void> deleteSpot(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        tourismService.deleteSpot(id, userId, role);
        return Result.ok("删除成功", null);
    }

    /**
     * 我发布的旅游项目（所有状态，包括待审核和被拒绝的）
     */
    @GetMapping("/spots/mine")
    public Result<PageResult<TourismSpotResponse>> getMySpots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(tourismService.getMySpots(userId, page, size));
    }

    // ==================== 需登录 — 评论 ====================

    /**
     * 发表评论（每人每景点限一次）
     */
    @PostMapping("/spots/{spotId}/reviews")
    public Result<TourismReviewResponse> createReview(@PathVariable Long spotId,
                                                       @Valid @RequestBody TourismReviewRequest request,
                                                       Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("评论成功", tourismService.createReview(spotId, userId, request));
    }

    /**
     * 删除评论（评论者本人或管理员）
     */
    @DeleteMapping("/reviews/{reviewId}")
    public Result<Void> deleteReview(@PathVariable Long reviewId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        tourismService.deleteReview(reviewId, userId, role);
        return Result.ok("评论已删除", null);
    }

    // ==================== 需登录 — 收藏 ====================

    /**
     * 收藏/取消收藏（toggle）
     *
     * 返回: { "data": { "favorited": true } }  → 收藏成功
     *       { "data": { "favorited": false } } → 已取消收藏
     */
    @PostMapping("/spots/{spotId}/favorite")
    public Result<Map<String, Boolean>> toggleFavorite(@PathVariable Long spotId,
                                                        Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean favorited = tourismService.toggleFavorite(spotId, userId);
        String msg = favorited ? "已收藏" : "已取消收藏";
        return Result.ok(msg, Map.of("favorited", favorited));
    }

    /**
     * 我的收藏列表
     */
    @GetMapping("/favorites")
    public Result<PageResult<TourismSpotResponse>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(tourismService.getMyFavorites(userId, page, size));
    }

    // ==================== 工具方法 ====================

    /** 从 Authentication 提取 userId（未登录返回 null） */
    private Long extractUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    /** 从 Authentication 提取真实角色（跳过通用的 ROLE_USER，取 ROLE_ADMIN/MERCHANT/FARMER/TOURIST） */
    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_") && !"ROLE_USER".equals(a))
                .map(a -> a.substring(5))  // "ROLE_ADMIN" → "ADMIN"
                .findFirst()
                .orElse("TOURIST");
    }
}
