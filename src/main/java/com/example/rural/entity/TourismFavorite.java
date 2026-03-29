package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 旅游收藏实体
 *
 * 数据库表: tourism_favorite
 *
 * 用户对景点的收藏关系，一个用户对同一景点只能收藏一次。
 * 收藏/取消收藏采用 toggle 模式：已收藏则取消，未收藏则添加。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_spot_fav",
                columnNames = {"user_id", "spot_id"}
        ),
        indexes = {
                @Index(name = "idx_tf_user", columnList = "user_id"),
                @Index(name = "idx_tf_spot", columnList = "spot_id")
        }
)
public class TourismFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 旅游项目 ID */
    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
