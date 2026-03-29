package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 旅游评论响应
 *
 * 评论区展示:
 *   ┌────────────────────────────────┐
 *   │  [头像] 用户昵称   ⭐⭐⭐⭐☆   │
 *   │  评论内容...                    │
 *   │  [图1] [图2]                    │
 *   │  2026-02-11 14:30              │
 *   └────────────────────────────────┘
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourismReviewResponse {

    private Long id;
    private Long spotId;

    // ---- 评论者信息 ----
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;

    // ---- 评论内容 ----
    private Integer rating;
    private String content;
    private String images;

    private LocalDateTime createdAt;
}
