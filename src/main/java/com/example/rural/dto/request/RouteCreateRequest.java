package com.example.rural.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建/编辑路线请求
 *
 * 前端调用:
 *   POST /api/v1/tourism/routes        创建路线
 *   PUT  /api/v1/tourism/routes/{id}   编辑路线
 *
 * 前端路线编辑器的数据格式:
 * {
 *   "title": "烟台三日游",
 *   "description": "田园+采摘+民宿，带娃度周末",
 *   "totalDays": 3,
 *   "startDate": "2026-03-20",
 *   "isPublic": true,
 *   "items": [
 *     { "spotId": 1, "dayNumber": 1, "sortOrder": 1, "visitTime": "09:00", "durationMinutes": 120, "notes": "上午摘草莓" },
 *     { "spotId": 3, "dayNumber": 1, "sortOrder": 2, "visitTime": "14:00", "durationMinutes": 180, "notes": "下午体验竹编" },
 *     { "spotId": 2, "dayNumber": 2, "sortOrder": 1, "notes": "住一晚看星空" },
 *     { "spotId": 5, "dayNumber": 3, "sortOrder": 1, "visitTime": "09:00", "durationMinutes": 240 }
 *   ]
 * }
 */
@Data
public class RouteCreateRequest {

    @NotBlank(message = "路线名称不能为空")
    private String title;

    private String description;

    private Integer totalDays = 1;

    /** 计划出发日期（可选） */
    private LocalDate startDate;

    /** 是否公开 */
    private Boolean isPublic = false;

    /** 路线行程项列表 */
    private List<RouteItemDTO> items;

    /**
     * 行程项
     */
    @Data
    public static class RouteItemDTO {

        /** 景点ID */
        private Long spotId;

        /** 第几天 */
        private Integer dayNumber = 1;

        /** 当天内顺序 */
        private Integer sortOrder = 1;

        /** 到达时间，如 "09:00" */
        private String visitTime;

        /** 停留时长（分钟） */
        private Integer durationMinutes;

        /** 备注 */
        private String notes;
    }
}
