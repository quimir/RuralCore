package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 旅游项目响应
 *
 * 前端展示:
 *
 * 列表页（卡片）:
 *   ┌──────────────────────────────────────┐
 *   │  [封面图]                              │
 *   │  标题                    ⭐ 4.5 (28)  │
 *   │  摘要...                              │
 *   │  📍 山东烟台牟平区    🎫 免费          │
 *   │  #亲子游 #周末好去处  👁 1234  ❤ 56   │
 *   └──────────────────────────────────────┘
 *
 * 详情页: 完整 content + 图片轮播 + 地图 + 评论区
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourismSpotResponse {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String type;
    private String coverImage;
    private String images;

    // ---- 位置信息 ----
    private String address;
    private Double longitude;
    private Double latitude;

    // ---- 联系与开放信息 ----
    private String contactPhone;
    private String openingHours;
    private BigDecimal ticketPrice;

    // ---- 发布者 ----
    private Long publisherId;
    private String publisherName;
    private String publisherAvatar;

    // ---- 统计数据 ----
    private Integer viewCount;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Integer favoriteCount;

    // ---- 当前用户状态（登录时返回） ----
    /** 当前用户是否已收藏 */
    private Boolean favorited;
    /** 当前用户是否已评论 */
    private Boolean reviewed;

    // ---- 管理信息 ----
    private String status;
    private String rejectReason;
    private String tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
