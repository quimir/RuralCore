package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅游路线实体（路线头）
 *
 * 数据库表: tourism_route
 *
 * 用户可以规划多日旅游路线，选择要去的景点，安排每天的行程。
 *
 * 使用场景:
 *   1. 用户浏览景点时点「加入行程」
 *   2. 在「我的路线」页面拖拽排序、分配到某一天
 *   3. 路线可以设为公开，供其他用户参考
 *   4. 系统推荐热门路线
 *
 * 关联关系:
 *   tourism_route.creator_id → user.id
 *   tourism_route_item.route_id → tourism_route.id（一对多）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_route", indexes = {
        @Index(name = "idx_route_creator", columnList = "creator_id"),
        @Index(name = "idx_route_public", columnList = "is_public")
})
public class TourismRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 路线名称 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 路线描述 */
    @Column(length = 1000)
    private String description;

    /** 创建者 */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /** 总天数 */
    @Column(name = "total_days", nullable = false)
    @Builder.Default
    private Integer totalDays = 1;

    /** 计划出发日期（可选） */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * 是否公开
     *   true  — 出现在「推荐路线」列表，所有人可见
     *   false — 仅自己可见
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /** 封面图（取路线中第一个景点的封面，或用户自定义） */
    @Column(name = "cover_image", length = 500)
    private String coverImage;

    /** 被收藏/引用次数（公开路线的热度指标） */
    @Column(name = "copy_count")
    @Builder.Default
    private Integer copyCount = 0;

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
}
