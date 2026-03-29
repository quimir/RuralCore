package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅游预约响应
 *
 * 前端预约详情页:
 *   ┌───────────────────────────────────────────────┐
 *   │  预约码: A3K7M2        状态: 已确认 ✅          │
 *   │  ──────────────────────────────────────────── │
 *   │  景点: 张三生态采摘园                           │
 *   │  票型: 成人票                                   │
 *   │  日期: 2026-03-15                              │
 *   │  数量: 2人    单价: ¥68    总计: ¥136           │
 *   │  ──────────────────────────────────────────── │
 *   │  联系人: 李四  13900139000                      │
 *   │  备注: 带2个小朋友                              │
 *   └───────────────────────────────────────────────┘
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String bookingCode;
    private String status;

    // ---- 景点与票型 ----
    private Long spotId;
    private String spotName;
    private String spotCoverImage;
    private Long ticketId;
    private String ticketName;
    private String ticketCategory;

    // ---- 预约信息 ----
    private LocalDate visitDate;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer quantity;
    private Integer nights;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    // ---- 联系人 ----
    private String contactName;
    private String contactPhone;
    private String remark;

    // ---- 时间线 ----
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;
}
