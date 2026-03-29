package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.BookingCreateRequest;
import com.example.rural.dto.request.TicketTypeRequest;
import com.example.rural.dto.response.BookingResponse;
import com.example.rural.dto.response.TicketTypeResponse;
import com.example.rural.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 旅游预约控制器
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  接口一览                                                               │
 * │                                                                         │
 * │  【票务配置 — 景点所有者/管理员】                                          │
 * │  POST   /api/v1/tourism/spots/{spotId}/tickets           添加票型        │
 * │  PUT    /api/v1/tourism/spots/{spotId}/tickets/{ticketId} 编辑票型       │
 * │  DELETE /api/v1/tourism/spots/{spotId}/tickets/{ticketId} 删除票型       │
 * │                                                                         │
 * │  【票型查询 — 公开】                                                     │
 * │  GET    /api/v1/tourism/spots/{spotId}/tickets?date=2026-03-15          │
 * │                                                                         │
 * │  【在线预约 — 登录用户】                                                  │
 * │  POST   /api/v1/tourism/bookings              创建预约                   │
 * │  GET    /api/v1/tourism/bookings               我的预约列表               │
 * │  GET    /api/v1/tourism/bookings/{id}          预约详情                   │
 * │  PUT    /api/v1/tourism/bookings/{id}/pay      模拟支付                   │
 * │  PUT    /api/v1/tourism/bookings/{id}/cancel   取消预约                   │
 * │                                                                         │
 * │  【核验 — 商家/管理员】                                                   │
 * │  PUT    /api/v1/tourism/bookings/verify        核验预约码                 │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/tourism")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ==================== 票务配置 ====================

    /** 添加票型 */
    @PostMapping("/spots/{spotId}/tickets")
    public Result<TicketTypeResponse> createTicket(@PathVariable Long spotId,
                                                    @Valid @RequestBody TicketTypeRequest request,
                                                    Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return Result.ok("票型创建成功", bookingService.createTicket(spotId, userId, role, request));
    }

    /** 编辑票型 */
    @PutMapping("/spots/{spotId}/tickets/{ticketId}")
    public Result<TicketTypeResponse> updateTicket(@PathVariable Long spotId,
                                                    @PathVariable Long ticketId,
                                                    @Valid @RequestBody TicketTypeRequest request,
                                                    Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return Result.ok(bookingService.updateTicket(ticketId, userId, role, request));
    }

    /** 删除票型 */
    @DeleteMapping("/spots/{spotId}/tickets/{ticketId}")
    public Result<Void> deleteTicket(@PathVariable Long spotId,
                                      @PathVariable Long ticketId,
                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        bookingService.deleteTicket(ticketId, userId, role);
        return Result.ok("票型已删除", null);
    }

    /**
     * 查看景点的票型列表（公开）
     *
     * 传 date 参数可查看指定日期的剩余量:
     * GET /api/v1/tourism/spots/1/tickets?date=2026-03-15
     */
    @GetMapping("/spots/{spotId}/tickets")
    public Result<List<TicketTypeResponse>> getSpotTickets(
            @PathVariable Long spotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(bookingService.getSpotTickets(spotId, date));
    }

    // ==================== 在线预约 ====================

    /** 创建预约 */
    @PostMapping("/bookings")
    public Result<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request,
                                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("预约成功", bookingService.createBooking(userId, request));
    }

    /** 我的预约列表 */
    @GetMapping("/bookings")
    public Result<PageResult<BookingResponse>> getMyBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(bookingService.getMyBookings(userId, status, page, size));
    }

    /** 预约详情 */
    @GetMapping("/bookings/{id}")
    public Result<BookingResponse> getBookingDetail(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(bookingService.getBookingDetail(id, userId));
    }

    /** 模拟支付 */
    @PutMapping("/bookings/{id}/pay")
    public Result<Void> payBooking(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        bookingService.payBooking(id, userId);
        return Result.ok("支付成功", null);
    }

    /** 取消预约 */
    @PutMapping("/bookings/{id}/cancel")
    public Result<Void> cancelBooking(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        bookingService.cancelBooking(id, userId);
        return Result.ok("已取消预约", null);
    }

    /**
     * 商家核验预约码
     *
     * PUT /api/v1/tourism/bookings/verify
     * Body: { "bookingCode": "A3K7M2" }
     */
    @PutMapping("/bookings/verify")
    public Result<BookingResponse> verifyBooking(@RequestBody Map<String, String> body,
                                                  Authentication auth) {
        String code = body.get("bookingCode");
        if (code == null || code.isBlank()) {
            return Result.error(400, "请提供预约码");
        }
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return Result.ok("核验成功", bookingService.verifyBooking(code, userId, role));
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_") && !"ROLE_USER".equals(a))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("TOURIST");
    }
}
