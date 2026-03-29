package com.example.rural.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 旅游预约请求
 *
 * 前端调用: POST /api/v1/tourism/bookings
 *
 * 门票/体验类:
 * {
 *   "ticketId": 1,
 *   "visitDate": "2026-03-15",
 *   "quantity": 2,
 *   "contactName": "李四",
 *   "contactPhone": "13900139000"
 * }
 *
 * 住宿类:
 * {
 *   "ticketId": 5,
 *   "checkIn": "2026-03-15",
 *   "checkOut": "2026-03-17",
 *   "quantity": 1,
 *   "contactName": "李四",
 *   "contactPhone": "13900139000"
 * }
 */
@Data
public class BookingCreateRequest {

    @NotNull(message = "票型ID不能为空")
    private Long ticketId;

    /** 门票/体验类: 游玩日期 */
    private LocalDate visitDate;

    /** 住宿类: 入住日期 */
    private LocalDate checkIn;

    /** 住宿类: 退房日期 */
    private LocalDate checkOut;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系人电话不能为空")
    private String contactPhone;

    private String remark;
}
