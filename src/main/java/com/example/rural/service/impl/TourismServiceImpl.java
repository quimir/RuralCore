package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.*;
import com.example.rural.dto.response.TourismReviewResponse;
import com.example.rural.dto.response.TourismSpotResponse;
import com.example.rural.entity.TourismFavorite;
import com.example.rural.entity.TourismReview;
import com.example.rural.entity.TourismSpot;
import com.example.rural.entity.User;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.TourismFavoriteRepository;
import com.example.rural.repository.TourismReviewRepository;
import com.example.rural.repository.TourismSpotRepository;
import com.example.rural.repository.UserRepository;
import com.example.rural.service.TourismService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourismServiceImpl implements TourismService {

    private final TourismSpotRepository spotRepository;
    private final TourismReviewRepository reviewRepository;
    private final TourismFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    // ==================== 景点 CRUD ====================

    /**
     * 发布旅游项目
     *
     * 权限逻辑:
     *   ADMIN → 直接 APPROVED（跳过审核）
     *   MERCHANT / FARMER → PENDING（等管理员审核）
     *   TOURIST → 拒绝
     */
    @Override
    @Transactional
    public TourismSpotResponse createSpot(Long publisherId, String role, TourismSpotRequest request) {
        // 权限检查：只有商家、农户、管理员可以发布
        if ("TOURIST".equals(role)) {
            throw new BusinessException(403, "游客角色不能发布旅游项目，请先升级为商家或农户");
        }

        TourismSpot.Type typeEnum;
        try {
            typeEnum = TourismSpot.Type.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的旅游类型: " + request.getType()
                    + "，可选值: SCENIC_SPOT, FARMSTAY, PICKING_GARDEN, AGRI_EXPERIENCE, FOLK_CULTURE");
        }

        // ========== 重复检测 ==========
        // 1. 同一发布者不能发布同名项目（已拒绝的除外，允许重新发布）
        if (spotRepository.existsByPublisherIdAndTitleAndStatusNot(
                publisherId, request.getTitle().trim(), TourismSpot.Status.REJECTED)) {
            throw new BusinessException("您已发布过同名旅游项目「" + request.getTitle() + "」，请勿重复发布");
        }

        // 2. 全局去重：同名 + 同地址视为同一个景点（不同人也不能重复发布）
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            if (spotRepository.existsByTitleAndAddressAndStatusNot(
                    request.getTitle().trim(), request.getAddress().trim(), TourismSpot.Status.REJECTED)) {
                throw new BusinessException("已存在同名同地址的旅游项目「" + request.getTitle()
                        + "」（" + request.getAddress() + "），请勿重复发布");
            }
        }

        // 管理员直接上线，其他角色需要审核
        TourismSpot.Status initialStatus = "ADMIN".equals(role)
                ? TourismSpot.Status.APPROVED
                : TourismSpot.Status.PENDING;

        TourismSpot spot = TourismSpot.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .content(request.getContent())
                .type(typeEnum)
                .coverImage(request.getCoverImage())
                .images(request.getImages())
                .address(request.getAddress())
                .longitude(request.getLongitude())
                .latitude(request.getLatitude())
                .contactPhone(request.getContactPhone())
                .openingHours(request.getOpeningHours())
                .ticketPrice(request.getTicketPrice() != null ? request.getTicketPrice() : BigDecimal.ZERO)
                .publisherId(publisherId)
                .status(initialStatus)
                .tags(request.getTags())
                .build();

        TourismSpot saved = spotRepository.save(spot);
        log.info("旅游项目发布: id={}, title={}, status={}", saved.getId(), saved.getTitle(), initialStatus);

        return toSpotResponse(saved, null);
    }

    @Override
    @Transactional
    public TourismSpotResponse updateSpot(Long spotId, Long currentUserId, String role,
                                           TourismSpotRequest request) {
        TourismSpot spot = findSpotOrThrow(spotId);
        checkOwnerOrAdmin(spot, currentUserId, role);

        TourismSpot.Type typeEnum;
        try {
            typeEnum = TourismSpot.Type.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的旅游类型: " + request.getType());
        }

        // ========== 重复检测（编辑时排除自身） ==========
        String newTitle = request.getTitle().trim();
        if (!newTitle.equals(spot.getTitle())) {
            // 标题变了才需要检查
            if (spotRepository.existsByPublisherIdAndTitleAndIdNotAndStatusNot(
                    spot.getPublisherId(), newTitle, spotId, TourismSpot.Status.REJECTED)) {
                throw new BusinessException("您已有同名旅游项目「" + newTitle + "」，请更换标题");
            }
            if (request.getAddress() != null && !request.getAddress().isBlank()) {
                if (spotRepository.existsByTitleAndAddressAndIdNotAndStatusNot(
                        newTitle, request.getAddress().trim(), spotId, TourismSpot.Status.REJECTED)) {
                    throw new BusinessException("已存在同名同地址的旅游项目「" + newTitle + "」");
                }
            }
        }

        spot.setTitle(newTitle);
        spot.setSummary(request.getSummary());
        spot.setContent(request.getContent());
        spot.setType(typeEnum);
        spot.setCoverImage(request.getCoverImage());
        spot.setImages(request.getImages());
        spot.setAddress(request.getAddress());
        spot.setLongitude(request.getLongitude());
        spot.setLatitude(request.getLatitude());
        spot.setContactPhone(request.getContactPhone());
        spot.setOpeningHours(request.getOpeningHours());
        spot.setTags(request.getTags());
        if (request.getTicketPrice() != null) {
            spot.setTicketPrice(request.getTicketPrice());
        }

        // 非管理员编辑后重新进入待审核状态
        if (!"ADMIN".equals(role) && spot.getStatus() == TourismSpot.Status.APPROVED) {
            spot.setStatus(TourismSpot.Status.PENDING);
            log.info("旅游项目编辑后重新待审核: id={}", spotId);
        }

        TourismSpot saved = spotRepository.save(spot);
        return toSpotResponse(saved, currentUserId);
    }

    @Override
    @Transactional
    public void deleteSpot(Long spotId, Long currentUserId, String role) {
        TourismSpot spot = findSpotOrThrow(spotId);
        checkOwnerOrAdmin(spot, currentUserId, role);
        spotRepository.delete(spot);
        log.info("旅游项目删除: id={}, title={}", spotId, spot.getTitle());
    }

    /**
     * 景点详情
     *
     * 公开可访问（APPROVED 状态），浏览量自动+1
     * 如果用户已登录，返回收藏和评论状态
     */
    @Override
    @Transactional
    public TourismSpotResponse getSpotDetail(Long spotId, Long currentUserId) {
        TourismSpot spot = findSpotOrThrow(spotId);

        // 非已通过的只有发布者和管理员可看（这里简化处理，直接检查状态）
        if (spot.getStatus() != TourismSpot.Status.APPROVED) {
            if (currentUserId == null || !spot.getPublisherId().equals(currentUserId)) {
                throw new BusinessException(404, "旅游项目不存在或未通过审核");
            }
        }

        // 浏览量 +1
        spot.setViewCount(spot.getViewCount() + 1);
        spotRepository.save(spot);

        return toSpotResponse(spot, currentUserId);
    }

    /**
     * 景点列表（公开）
     *
     * 只展示 APPROVED 状态的项目，支持:
     *   - 关键词搜索（标题、摘要、地址）
     *   - 类型筛选
     *   - 价格区间
     *   - 多种排序方式
     */
    @Override
    public PageResult<TourismSpotResponse> listSpots(TourismSpotQueryRequest query, Long currentUserId) {
        Specification<TourismSpot> spec = buildPublicSpec(query);
        Sort sort = buildSort(query.getSort());
        PageRequest pageable = PageRequest.of(query.getPage() - 1, query.getSize(), sort);

        Page<TourismSpot> page = spotRepository.findAll(spec, pageable);
        return PageResult.from(page, spot -> toSpotResponse(spot, currentUserId));
    }

    @Override
    public PageResult<TourismSpotResponse> getMySpots(Long publisherId, int page, int size) {
        // 用 Specification 查自己发布的所有状态
        Specification<TourismSpot> spec = (root, q, cb) ->
                cb.equal(root.get("publisherId"), publisherId);

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TourismSpot> pageResult = spotRepository.findAll(spec, pageable);
        return PageResult.from(pageResult, spot -> toSpotResponse(spot, publisherId));
    }

    // ==================== 管理员审核 ====================

    @Override
    @Transactional
    public TourismSpotResponse auditSpot(Long spotId, TourismAuditRequest request) {
        TourismSpot spot = findSpotOrThrow(spotId);

        if (spot.getStatus() != TourismSpot.Status.PENDING) {
            throw new BusinessException("只有待审核状态的项目可以审核，当前状态: " + spot.getStatus());
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            spot.setStatus(TourismSpot.Status.APPROVED);
            spot.setRejectReason(null);
            log.info("旅游项目审核通过: id={}, title={}", spotId, spot.getTitle());
        } else {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new BusinessException("拒绝时必须填写理由");
            }
            spot.setStatus(TourismSpot.Status.REJECTED);
            spot.setRejectReason(request.getReason());
            log.info("旅游项目审核拒绝: id={}, reason={}", spotId, request.getReason());
        }

        TourismSpot saved = spotRepository.save(spot);
        return toSpotResponse(saved, null);
    }

    @Override
    public PageResult<TourismSpotResponse> adminListSpots(String status, int page, int size) {
        Specification<TourismSpot> spec = (root, q, cb) -> {
            if (status != null && !status.isBlank()) {
                try {
                    TourismSpot.Status statusEnum = TourismSpot.Status.valueOf(status.toUpperCase());
                    return cb.equal(root.get("status"), statusEnum);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("无效状态: " + status);
                }
            }
            return null; // 不加条件 = 查全部
        };

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TourismSpot> pageResult = spotRepository.findAll(spec, pageable);
        return PageResult.from(pageResult, spot -> toSpotResponse(spot, null));
    }

    // ==================== 评论 ====================

    /**
     * 发表评论
     *
     * 每人对每个景点只能评论一次（数据库唯一约束兜底）
     * 发评论后自动更新景点的 avg_rating 和 review_count
     */
    @Override
    @Transactional
    public TourismReviewResponse createReview(Long spotId, Long userId, TourismReviewRequest request) {
        TourismSpot spot = findSpotOrThrow(spotId);
        if (spot.getStatus() != TourismSpot.Status.APPROVED) {
            throw new BusinessException("只能评论已上线的旅游项目");
        }

        // 检查是否已评论
        if (reviewRepository.findByUserIdAndSpotId(userId, spotId).isPresent()) {
            throw new BusinessException("您已经评论过该景点了");
        }

        TourismReview review = TourismReview.builder()
                .spotId(spotId)
                .userId(userId)
                .rating(request.getRating())
                .content(request.getContent())
                .images(request.getImages())
                .build();

        TourismReview saved = reviewRepository.save(review);

        // 更新景点的评分和评论数
        refreshSpotStats(spotId);

        return toReviewResponse(saved);
    }

    @Override
    public PageResult<TourismReviewResponse> getSpotReviews(Long spotId, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<TourismReview> pageResult = reviewRepository
                .findBySpotIdAndVisibleTrueOrderByCreatedAtDesc(spotId, pageable);
        return PageResult.from(pageResult, this::toReviewResponse);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId, String role) {
        TourismReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));

        // 只有评论者本人或管理员可以删除
        if (!review.getUserId().equals(currentUserId) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "无权删除此评论");
        }

        Long spotId = review.getSpotId();
        reviewRepository.delete(review);

        // 更新景点统计
        refreshSpotStats(spotId);
        log.info("评论删除: reviewId={}, spotId={}", reviewId, spotId);
    }

    // ==================== 收藏 ====================

    /**
     * 切换收藏状态（toggle）
     * 已收藏 → 取消收藏（返回 false）
     * 未收藏 → 添加收藏（返回 true）
     */
    @Override
    @Transactional
    public boolean toggleFavorite(Long spotId, Long userId) {
        findSpotOrThrow(spotId); // 确保景点存在

        return favoriteRepository.findByUserIdAndSpotId(userId, spotId)
                .map(existing -> {
                    // 已收藏 → 取消
                    favoriteRepository.delete(existing);
                    refreshFavoriteCount(spotId);
                    return false;
                })
                .orElseGet(() -> {
                    // 未收藏 → 添加
                    TourismFavorite fav = TourismFavorite.builder()
                            .userId(userId)
                            .spotId(spotId)
                            .build();
                    favoriteRepository.save(fav);
                    refreshFavoriteCount(spotId);
                    return true;
                });
    }

    @Override
    public PageResult<TourismSpotResponse> getMyFavorites(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<TourismFavorite> favPage = favoriteRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 将收藏记录转为景点详情
        List<TourismSpotResponse> items = favPage.getContent().stream()
                .map(fav -> spotRepository.findById(fav.getSpotId()).orElse(null))
                .filter(spot -> spot != null)
                .map(spot -> toSpotResponse(spot, userId))
                .toList();

        return PageResult.<TourismSpotResponse>builder()
                .records(items)
                .total(favPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    // ==================== 工具方法 ====================

    private TourismSpot findSpotOrThrow(Long spotId) {
        return spotRepository.findById(spotId)
                .orElseThrow(() -> new BusinessException(404, "旅游项目不存在"));
    }

    private void checkOwnerOrAdmin(TourismSpot spot, Long currentUserId, String role) {
        if (!"ADMIN".equals(role) && !spot.getPublisherId().equals(currentUserId)) {
            throw new BusinessException(403, "无权操作此旅游项目");
        }
    }

    /** 刷新景点的 avg_rating 和 review_count */
    private void refreshSpotStats(Long spotId) {
        spotRepository.findById(spotId).ifPresent(spot -> {
            Double avg = reviewRepository.getAverageRating(spotId);
            long count = reviewRepository.countBySpotIdAndVisibleTrue(spotId);

            spot.setAvgRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            spot.setReviewCount((int) count);
            spotRepository.save(spot);
        });
    }

    /** 刷新景点的收藏数 */
    private void refreshFavoriteCount(Long spotId) {
        spotRepository.findById(spotId).ifPresent(spot -> {
            spot.setFavoriteCount((int) favoriteRepository.countBySpotId(spotId));
            spotRepository.save(spot);
        });
    }

    /**
     * 公开列表的查询条件（JPA Specification 动态拼接 WHERE）
     *
     * 固定条件: status = APPROVED
     * 可选条件: keyword LIKE / type = / ticketPrice BETWEEN
     */
    private Specification<TourismSpot> buildPublicSpec(TourismSpotQueryRequest query) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 必须是已通过审核的
            predicates.add(cb.equal(root.get("status"), TourismSpot.Status.APPROVED));

            // 关键词（匹配标题、摘要、地址）
            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                String kw = "%" + query.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("summary"), kw),
                        cb.like(root.get("address"), kw)
                ));
            }

            // 类型筛选
            if (query.getType() != null && !query.getType().isBlank()) {
                try {
                    TourismSpot.Type typeEnum = TourismSpot.Type.valueOf(query.getType().toUpperCase());
                    predicates.add(cb.equal(root.get("type"), typeEnum));
                } catch (IllegalArgumentException ignored) {
                    // 无效类型忽略
                }
            }

            // 价格区间
            if (query.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ticketPrice"), query.getMinPrice()));
            }
            if (query.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("ticketPrice"), query.getMaxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 排序 */
    private Sort buildSort(String sortStr) {
        if (sortStr == null) sortStr = "newest";
        return switch (sortStr) {
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "views_desc" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "ticketPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "ticketPrice");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // newest
        };
    }

    /** TourismSpot → Response（附带发布者信息 + 当前用户状态） */
    private TourismSpotResponse toSpotResponse(TourismSpot spot, Long currentUserId) {
        // 发布者信息
        User publisher = userRepository.findById(spot.getPublisherId()).orElse(null);
        String publisherName = publisher != null ? publisher.getNickname() : "未知";
        String publisherAvatar = publisher != null ? publisher.getAvatar() : null;

        // 当前用户状态
        Boolean favorited = null;
        Boolean reviewed = null;
        if (currentUserId != null) {
            favorited = favoriteRepository.existsByUserIdAndSpotId(currentUserId, spot.getId());
            reviewed = reviewRepository.findByUserIdAndSpotId(currentUserId, spot.getId()).isPresent();
        }

        return TourismSpotResponse.builder()
                .id(spot.getId())
                .title(spot.getTitle())
                .summary(spot.getSummary())
                .content(spot.getContent())
                .type(spot.getType().name())
                .coverImage(spot.getCoverImage())
                .images(spot.getImages())
                .address(spot.getAddress())
                .longitude(spot.getLongitude())
                .latitude(spot.getLatitude())
                .contactPhone(spot.getContactPhone())
                .openingHours(spot.getOpeningHours())
                .ticketPrice(spot.getTicketPrice())
                .publisherId(spot.getPublisherId())
                .publisherName(publisherName)
                .publisherAvatar(publisherAvatar)
                .viewCount(spot.getViewCount())
                .avgRating(spot.getAvgRating())
                .reviewCount(spot.getReviewCount())
                .favoriteCount(spot.getFavoriteCount())
                .favorited(favorited)
                .reviewed(reviewed)
                .status(spot.getStatus().name())
                .rejectReason(spot.getRejectReason())
                .tags(spot.getTags())
                .createdAt(spot.getCreatedAt())
                .updatedAt(spot.getUpdatedAt())
                .build();
    }

    /** TourismReview → Response（附带用户信息） */
    private TourismReviewResponse toReviewResponse(TourismReview review) {
        User user = userRepository.findById(review.getUserId()).orElse(null);

        return TourismReviewResponse.builder()
                .id(review.getId())
                .spotId(review.getSpotId())
                .userId(review.getUserId())
                .username(user != null ? user.getUsername() : "已注销")
                .nickname(user != null ? user.getNickname() : "已注销")
                .avatar(user != null ? user.getAvatar() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .images(review.getImages())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
