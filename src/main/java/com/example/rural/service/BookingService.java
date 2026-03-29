package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.BookingCreateRequest;
import com.example.rural.dto.request.TicketTypeRequest;
import com.example.rural.dto.response.BookingResponse;
import com.example.rural.dto.response.TicketTypeResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 旅游预约服务
 *
 * 两大功能:
 *   1. 票务管理（商家/农户配置自己景点的票型和容量）
 *   2. 在线预约（用户选票型→选日期→下单→到场核验）
 */
public interface BookingService {

    // ==================== 票务配置（景点所有者） ====================

    /** 添加票型 */
    TicketTypeResponse createTicket(Long spotId, Long currentUserId, String role, TicketTypeRequest request);

    /** 编辑票型 */
    TicketTypeResponse updateTicket(Long ticketId, Long currentUserId, String role, TicketTypeRequest request);

    /** 删除票型 */
    void deleteTicket(Long ticketId, Long currentUserId, String role);

    /** 查看景点的票型列表（公开，含指定日期的剩余量） */
    List<TicketTypeResponse> getSpotTickets(Long spotId, LocalDate date);

    // ==================== 在线预约（用户） ====================

    /** 创建预约 */
    BookingResponse createBooking(Long userId, BookingCreateRequest request);

    /** 查看预约详情 */
    BookingResponse getBookingDetail(Long bookingId, Long currentUserId);

    /** 我的预约列表 */
    PageResult<BookingResponse> getMyBookings(Long userId, String status, int page, int size);

    /** 模拟支付 */
    void payBooking(Long bookingId, Long userId);

    /** 取消预约 */
    void cancelBooking(Long bookingId, Long userId);

    /** 商家核验预约码 → 标记已使用 */
    BookingResponse verifyBooking(String bookingCode, Long verifierId, String role);
}
