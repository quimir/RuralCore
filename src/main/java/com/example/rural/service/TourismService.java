package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.*;
import com.example.rural.dto.response.TourismReviewResponse;
import com.example.rural.dto.response.TourismSpotResponse;

/**
 * 旅游推广服务接口
 *
 * 覆盖三大功能: 景点管理 + 评论互动 + 收藏
 *
 * 权限说明（Service 层校验）:
 *   发布景点: 仅 MERCHANT / FARMER / ADMIN
 *   编辑景点: 发布者本人 或 ADMIN
 *   审核景点: 仅 ADMIN
 *   评论:     所有登录用户
 *   收藏:     所有登录用户
 */
public interface TourismService {

    // ==================== 景点 CRUD ====================

    /** 发布旅游项目（商家/农户→待审核，管理员→直接上线） */
    TourismSpotResponse createSpot(Long publisherId, String role, TourismSpotRequest request);

    /** 编辑旅游项目（所有者或管理员） */
    TourismSpotResponse updateSpot(Long spotId, Long currentUserId, String role, TourismSpotRequest request);

    /** 删除旅游项目（所有者或管理员） */
    void deleteSpot(Long spotId, Long currentUserId, String role);

    /** 景点详情（公开，浏览量+1） */
    TourismSpotResponse getSpotDetail(Long spotId, Long currentUserId);

    /** 景点列表（公开，只显示 APPROVED 状态） */
    PageResult<TourismSpotResponse> listSpots(TourismSpotQueryRequest query, Long currentUserId);

    /** 我发布的旅游项目（所有状态） */
    PageResult<TourismSpotResponse> getMySpots(Long publisherId, int page, int size);

    // ==================== 管理员审核 ====================

    /** 审核（通过/拒绝） */
    TourismSpotResponse auditSpot(Long spotId, TourismAuditRequest request);

    /** 管理员查看所有旅游项目（含待审核，支持状态筛选） */
    PageResult<TourismSpotResponse> adminListSpots(String status, int page, int size);

    // ==================== 评论 ====================

    /** 发表评论（每人对每个景点只能评论一次） */
    TourismReviewResponse createReview(Long spotId, Long userId, TourismReviewRequest request);

    /** 景点的评论列表（分页） */
    PageResult<TourismReviewResponse> getSpotReviews(Long spotId, int page, int size);

    /** 删除评论（评论者本人或管理员） */
    void deleteReview(Long reviewId, Long currentUserId, String role);

    // ==================== 收藏 ====================

    /** 切换收藏状态（已收藏→取消，未收藏→添加），返回操作后的收藏状态 */
    boolean toggleFavorite(Long spotId, Long userId);

    /** 我的收藏列表 */
    PageResult<TourismSpotResponse> getMyFavorites(Long userId, int page, int size);
}
