package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 旅游评论实体
 *
 * 数据库表: tourism_review
 *
 * 登录用户可以对已通过审核的旅游项目发表评论和打分。
 * 每个用户对同一景点只能评论一次（唯一约束: user_id + spot_id）。
 *
 * 评论发布后，会自动更新 tourism_spot 的 avg_rating 和 review_count。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_spot_review",
                columnNames = {"user_id", "spot_id"}
        ),
        indexes = {
                @Index(name = "idx_tr_spot", columnList = "spot_id"),
                @Index(name = "idx_tr_user", columnList = "user_id")
        }
)
public class TourismReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 旅游项目 ID */
    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    /** 评论者 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 评分（1-5 星） */
    @Column(nullable = false)
    private Integer rating;

    /** 评论内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 评论配图（JSON 数组）
     * 例: ["/img/review1.jpg", "/img/review2.jpg"]
     */
    @Column(length = 1000)
    private String images;

    /** 是否可见（管理员可隐藏违规评论） */
    @Column(nullable = false)
    @Builder.Default
    private Boolean visible = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
