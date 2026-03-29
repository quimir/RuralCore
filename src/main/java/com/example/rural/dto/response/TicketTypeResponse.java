package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 票型响应
 *
 * 前端景点详情页的预约面板:
 *
 *   选择票型:
 *   ┌─────────────────────────────────────┐
 *   │  成人票    ¥68/人   剩余 87 张  [选择] │
 *   │  儿童票    ¥38/人   剩余 92 张  [选择] │
 *   │  家庭套票  ¥158/组  剩余 25 组  [选择] │
 *   └─────────────────────────────────────┘
 *
 *   remaining 字段仅在前端传了查询日期时才计算，
 *   否则为 null（不显示余量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeResponse {

    private Long id;
    private Long spotId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer dailyCapacity;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Integer advanceDays;
    private Boolean enabled;

    /** 指定日期的剩余可预约量（动态计算，仅查询时填充） */
    private Integer remaining;
}
