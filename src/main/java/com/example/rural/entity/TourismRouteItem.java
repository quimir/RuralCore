package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路线行程项实体
 *
 * 数据库表: tourism_route_item
 *
 * 一条路线包含多个行程项，每个行程项属于某一天，
 * 在那一天中有排序顺序。
 *
 * 举例:
 *   "烟台三日游"路线:
 *     Day 1:
 *       sort=1  张三采摘园       09:00  停留2小时  "上午摘草莓"
 *       sort=2  古村落非遗体验   14:00  停留3小时  "下午体验竹编"
 *     Day 2:
 *       sort=1  云上山居民宿     全天   住宿       "住一晚看星空"
 *     Day 3:
 *       sort=1  某景区           09:00  停留4小时  "爬山"
 *
 * 关联关系:
 *   tourism_route_item.route_id → tourism_route.id
 *   tourism_route_item.spot_id  → tourism_spot.id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_route_item", indexes = {
        @Index(name = "idx_ri_route", columnList = "route_id")
})
public class TourismRouteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属路线 */
    @Column(name = "route_id", nullable = false)
    private Long routeId;

    /** 景点ID */
    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    /** 第几天（从1开始） */
    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    /** 当天内的顺序（从1开始） */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 建议到达时间，如 "09:00" */
    @Column(name = "visit_time", length = 20)
    private String visitTime;

    /** 建议停留时长（分钟） */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** 用户备注 */
    @Column(length = 500)
    private String notes;
}
