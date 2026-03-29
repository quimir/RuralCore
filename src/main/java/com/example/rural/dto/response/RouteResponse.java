package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 旅游路线响应
 *
 * 前端路线详情页:
 *
 *   烟台三日游                    ★ 已被 128 人引用
 *   ─────────────────────────────────────────
 *   📅 Day 1 — 3月20日
 *     09:00  [图] 张三生态采摘园     🕐 2小时
 *                 "上午摘草莓"
 *     14:00  [图] 古村落非遗体验     🕐 3小时
 *                 "下午体验竹编"
 *
 *   📅 Day 2 — 3月21日
 *     全天   [图] 云上山居民宿
 *                 "住一晚看星空"
 *
 *   📅 Day 3 — 3月22日
 *     09:00  [图] 田园风光景区       🕐 4小时
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private Long id;
    private String title;
    private String description;
    private Integer totalDays;
    private LocalDate startDate;
    private Boolean isPublic;
    private String coverImage;
    private Integer copyCount;

    // ---- 创建者 ----
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;

    /** 按天分组的行程 */
    private List<DayPlan> days;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 某一天的行程
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer dayNumber;
        private LocalDate date;  // startDate + dayNumber - 1
        private List<SpotItem> spots;
    }

    /**
     * 某天中的一个景点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotItem {
        private Long itemId;     // route_item.id（用于删除/修改）
        private Long spotId;
        private String spotName;
        private String spotCoverImage;
        private String spotType;
        private String address;
        private Integer sortOrder;
        private String visitTime;
        private Integer durationMinutes;
        private String notes;
    }
}
