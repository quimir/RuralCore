package com.example.rural.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票型配置请求
 *
 * 景点发布者使用:
 *   POST /api/v1/tourism/spots/{spotId}/tickets
 *   PUT  /api/v1/tourism/spots/{spotId}/tickets/{ticketId}
 */
@Data
public class TicketTypeRequest {

    @NotBlank(message = "票型名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal price;

    /** TICKET（门票/体验）或 ROOM（住宿） */
    @NotNull(message = "票型分类不能为空")
    private String category;

    @NotNull(message = "每日容量不能为空")
    @Min(value = 1, message = "每日容量至少为1")
    private Integer dailyCapacity;

    @Min(value = 1, message = "最少预约数量至少为1")
    private Integer minQuantity;

    @Min(value = 1, message = "最多预约数量至少为1")
    private Integer maxQuantity;

    /** 需提前几天预约 */
    private Integer advanceDays;
}
