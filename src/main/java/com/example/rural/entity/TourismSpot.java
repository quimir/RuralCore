package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 旅游项目实体
 *
 * 数据库表: tourism_spot
 *
 * 涵盖乡村旅游的各种业态：
 *   - SCENIC_SPOT     田园风光、古村落、自然景区
 *   - FARMSTAY        民宿、农家院
 *   - PICKING_GARDEN  采摘园（草莓/樱桃/葡萄…）
 *   - AGRI_EXPERIENCE 农事体验（插秧、磨豆腐、制茶…）
 *   - FOLK_CULTURE    民俗文化（赶集、庙会、非遗体验…）
 *
 * 审核流程:
 *   商家/农户发布 → PENDING（待审核）
 *   管理员审核 → APPROVED（已通过，前台可见）/ REJECTED（已拒绝）
 *   管理员发布 → 直接 APPROVED
 *   发布者主动下线 → OFFLINE
 *
 * 关联关系:
 *   tourism_spot.publisher_id → user.id
 *   tourism_review.spot_id → tourism_spot.id（一对多）
 *   tourism_favorite.spot_id → tourism_spot.id（一对多）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_spot", indexes = {
        @Index(name = "idx_ts_publisher", columnList = "publisher_id"),
        @Index(name = "idx_ts_type", columnList = "type"),
        @Index(name = "idx_ts_status", columnList = "status")
})
public class TourismSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 摘要描述（列表页显示） */
    @Column(length = 500)
    private String summary;

    /** 详细内容（富文本/Markdown，详情页显示） */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 旅游类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    /** 封面图 URL */
    @Column(name = "cover_image", length = 500)
    private String coverImage;

    /**
     * 多张图片（JSON 数组）
     * 例: ["/img/a.jpg", "/img/b.jpg", "/img/c.jpg"]
     * 前端传 JSON 字符串，后端直接存
     */
    @Column(columnDefinition = "TEXT")
    private String images;

    /** 详细地址 */
    @Column(length = 300)
    private String address;

    /** 经度（地图定位） */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 联系电话 */
    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    /** 开放时间（文本描述，如 "全天开放" 或 "08:00-18:00"） */
    @Column(name = "opening_hours", length = 100)
    private String openingHours;

    /**
     * 门票/价格
     * 0 表示免费
     * 民宿类表示起步价
     */
    @Column(name = "ticket_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal ticketPrice = BigDecimal.ZERO;

    /** 发布者 ID */
    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    /** 审核状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /** 审核拒绝理由 */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 浏览量 */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /** 平均评分（冗余字段，由评论计算更新） */
    @Column(name = "avg_rating", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    /** 评论数（冗余） */
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    /** 收藏数（冗余） */
    @Column(name = "favorite_count")
    @Builder.Default
    private Integer favoriteCount = 0;

    /**
     * 标签（JSON 数组）
     * 例: ["亲子游", "免费", "拍照打卡", "周末好去处"]
     */
    @Column(length = 500)
    private String tags;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举 ====================

    public enum Type {
        SCENIC_SPOT,      // 田园风光 / 自然景区
        FARMSTAY,         // 民宿 / 农家院
        PICKING_GARDEN,   // 采摘园
        AGRI_EXPERIENCE,  // 农事体验
        FOLK_CULTURE      // 民俗文化
    }

    public enum Status {
        PENDING,   // 待审核（商家/农户发布后默认）
        APPROVED,  // 已通过（前台可见）
        REJECTED,  // 已拒绝
        OFFLINE    // 已下线（发布者主动下线）
    }
}
