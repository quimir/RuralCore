package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.BookingCreateRequest;
import com.example.rural.dto.request.TicketTypeRequest;
import com.example.rural.dto.response.BookingResponse;
import com.example.rural.dto.response.TicketTypeResponse;
import com.example.rural.entity.TicketType;
import com.example.rural.entity.TourismBooking;
import com.example.rural.entity.TourismSpot;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.TicketTypeRepository;
import com.example.rural.repository.TourismBookingRepository;
import com.example.rural.repository.TourismSpotRepository;
import com.example.rural.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final TicketTypeRepository ticketRepository;
    private final TourismBookingRepository bookingRepository;
    private final TourismSpotRepository spotRepository;

    // ==================== 票务配置 ====================

    @Override
    @Transactional
    public TicketTypeResponse createTicket(Long spotId, Long currentUserId, String role,
                                            TicketTypeRequest request) {
        TourismSpot spot = findApprovedSpot(spotId);
        checkSpotOwnerOrAdmin(spot, currentUserId, role);

        // 票型分类校验
        TicketType.Category category = parseCategory(request.getCategory());

        // 同名检查
        if (ticketRepository.existsBySpotIdAndName(spotId, request.getName().trim())) {
            throw new BusinessException("该景点已有同名票型「" + request.getName() + "」");
        }

        TicketType ticket = TicketType.builder()
                .spotId(spotId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .dailyCapacity(request.getDailyCapacity())
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1)
                .maxQuantity(request.getMaxQuantity() != null ? request.getMaxQuantity() : 10)
                .advanceDays(request.getAdvanceDays() != null ? request.getAdvanceDays() : 0)
                .build();

        TicketType saved = ticketRepository.save(ticket);
        log.info("票型创建: spotId={}, name={}, price={}", spotId, saved.getName(), saved.getPrice());
        return toTicketResponse(saved, null);
    }

    @Override
    @Transactional
    public TicketTypeResponse updateTicket(Long ticketId, Long currentUserId, String role,
                                            TicketTypeRequest request) {
        TicketType ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(404, "票型不存在"));

        TourismSpot spot = findApprovedSpot(ticket.getSpotId());
        checkSpotOwnerOrAdmin(spot, currentUserId, role);

        TicketType.Category category = parseCategory(request.getCategory());

        if (ticketRepository.existsBySpotIdAndNameAndIdNot(
                ticket.getSpotId(), request.getName().trim(), ticketId)) {
            throw new BusinessException("该景点已有同名票型「" + request.getName() + "」");
        }

        ticket.setName(request.getName().trim());
        ticket.setDescription(request.getDescription());
        ticket.setPrice(request.getPrice());
        ticket.setCategory(category);
        ticket.setDailyCapacity(request.getDailyCapacity());
        if (request.getMinQuantity() != null) ticket.setMinQuantity(request.getMinQuantity());
        if (request.getMaxQuantity() != null) ticket.setMaxQuantity(request.getMaxQuantity());
        if (request.getAdvanceDays() != null) ticket.setAdvanceDays(request.getAdvanceDays());

        return toTicketResponse(ticketRepository.save(ticket), null);
    }

    @Override
    @Transactional
    public void deleteTicket(Long ticketId, Long currentUserId, String role) {
        TicketType ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(404, "票型不存在"));
        TourismSpot spot = findApprovedSpot(ticket.getSpotId());
        checkSpotOwnerOrAdmin(spot, currentUserId, role);
        ticketRepository.delete(ticket);
    }

    @Override
    public List<TicketTypeResponse> getSpotTickets(Long spotId, LocalDate date) {
        List<TicketType> tickets = ticketRepository.findBySpotIdAndEnabledTrue(spotId);
        return tickets.stream()
                .map(t -> toTicketResponse(t, date))
                .toList();
    }

    // ==================== 在线预约 ====================

    /**
     * 创建预约
     *
     * 核心流程:
     *   1. 校验票型存在且上架
     *   2. 校验日期合法（不能是过去、需提前N天）
     *   3. 校验数量在 [min, max] 范围内
     *   4. 检查指定日期的剩余容量
     *   5. 生成预约码
     *   6. 计算总金额
     *   7. 保存预约
     */
    @Override
    @Transactional
    public BookingResponse createBooking(Long userId, BookingCreateRequest request) {
        // 1. 查票型
        TicketType ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new BusinessException("票型不存在"));
        if (!ticket.getEnabled()) {
            throw new BusinessException("该票型暂不可预约");
        }

        TourismSpot spot = spotRepository.findById(ticket.getSpotId())
                .orElseThrow(() -> new BusinessException("景点不存在"));
        if (spot.getStatus() != TourismSpot.Status.APPROVED) {
            throw new BusinessException("景点未上线，暂不可预约");
        }

        // 2. 校验日期
        LocalDate visitDate;
        int nights = 0;

        if (ticket.getCategory() == TicketType.Category.ROOM) {
            // 住宿类
            if (request.getCheckIn() == null || request.getCheckOut() == null) {
                throw new BusinessException("住宿类票型必须选择入住和退房日期");
            }
            if (!request.getCheckOut().isAfter(request.getCheckIn())) {
                throw new BusinessException("退房日期必须晚于入住日期");
            }
            visitDate = request.getCheckIn();
            nights = (int) ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        } else {
            // 门票类
            if (request.getVisitDate() == null) {
                throw new BusinessException("门票/体验类票型必须选择游玩日期");
            }
            visitDate = request.getVisitDate();
        }

        if (visitDate.isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约过去的日期");
        }
        if (ticket.getAdvanceDays() > 0) {
            LocalDate earliest = LocalDate.now().plusDays(ticket.getAdvanceDays());
            if (visitDate.isBefore(earliest)) {
                throw new BusinessException("该票型需提前" + ticket.getAdvanceDays() + "天预约");
            }
        }

        // 3. 校验数量
        if (request.getQuantity() < ticket.getMinQuantity()) {
            throw new BusinessException("最少预约" + ticket.getMinQuantity() + "份");
        }
        if (request.getQuantity() > ticket.getMaxQuantity()) {
            throw new BusinessException("单次最多预约" + ticket.getMaxQuantity() + "份");
        }

        // 4. 检查余量
        int remaining;
        if (ticket.getCategory() == TicketType.Category.ROOM) {
            int booked = bookingRepository.sumOverlappingRoomBookings(
                    ticket.getId(), request.getCheckIn(), request.getCheckOut());
            remaining = ticket.getDailyCapacity() - booked;
        } else {
            int booked = bookingRepository.sumBookedQuantityByTicketAndDate(
                    ticket.getId(), visitDate);
            remaining = ticket.getDailyCapacity() - booked;
        }

        if (remaining < request.getQuantity()) {
            throw new BusinessException("该日期余量不足，剩余: " + Math.max(0, remaining));
        }

        // 5. 生成预约码
        String bookingCode = generateBookingCode();

        // 6. 计算金额
        int multiplier = nights > 0 ? nights : 1;
        BigDecimal totalAmount = ticket.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()))
                .multiply(BigDecimal.valueOf(multiplier));

        // 7. 保存
        TourismBooking booking = TourismBooking.builder()
                .userId(userId)
                .spotId(spot.getId())
                .ticketId(ticket.getId())
                .spotName(spot.getTitle())
                .ticketName(ticket.getName())
                .unitPrice(ticket.getPrice())
                .visitDate(ticket.getCategory() == TicketType.Category.TICKET ? visitDate : null)
                .checkIn(ticket.getCategory() == TicketType.Category.ROOM ? request.getCheckIn() : null)
                .checkOut(ticket.getCategory() == TicketType.Category.ROOM ? request.getCheckOut() : null)
                .quantity(request.getQuantity())
                .nights(nights)
                .totalAmount(totalAmount)
                .bookingCode(bookingCode)
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .remark(request.getRemark())
                .build();

        TourismBooking saved = bookingRepository.save(booking);
        log.info("预约创建: bookingCode={}, spot={}, ticket={}, date={}, qty={}",
                bookingCode, spot.getTitle(), ticket.getName(), visitDate, request.getQuantity());

        return toBookingResponse(saved);
    }

    @Override
    public BookingResponse getBookingDetail(Long bookingId, Long currentUserId) {
        TourismBooking booking = findBookingOrThrow(bookingId);
        // 预约者本人或景点所有者可查看
        if (!booking.getUserId().equals(currentUserId)) {
            TourismSpot spot = spotRepository.findById(booking.getSpotId()).orElse(null);
            if (spot == null || !spot.getPublisherId().equals(currentUserId)) {
                throw new BusinessException(403, "无权查看此预约");
            }
        }
        return toBookingResponse(booking);
    }

    @Override
    public PageResult<BookingResponse> getMyBookings(Long userId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<TourismBooking> pageResult;

        if (status != null && !status.isBlank()) {
            try {
                TourismBooking.Status statusEnum = TourismBooking.Status.valueOf(status.toUpperCase());
                pageResult = bookingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的预约状态: " + status);
            }
        } else {
            pageResult = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return PageResult.from(pageResult, this::toBookingResponse);
    }

    @Override
    @Transactional
    public void payBooking(Long bookingId, Long userId) {
        TourismBooking booking = findBookingOrThrow(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (booking.getStatus() != TourismBooking.Status.PENDING_PAYMENT) {
            throw new BusinessException("当前状态不允许支付");
        }
        booking.setStatus(TourismBooking.Status.CONFIRMED);
        booking.setPaidAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("预约已支付: bookingCode={}", booking.getBookingCode());
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        TourismBooking booking = findBookingOrThrow(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (booking.getStatus() != TourismBooking.Status.PENDING_PAYMENT
                && booking.getStatus() != TourismBooking.Status.CONFIRMED) {
            throw new BusinessException("当前状态不允许取消");
        }
        booking.setStatus(TourismBooking.Status.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("预约已取消: bookingCode={}", booking.getBookingCode());
    }

    /**
     * 商家核验预约码
     *
     * 到场核验流程:
     *   1. 游客出示预约码 A3K7M2
     *   2. 商家在后台输入预约码
     *   3. 系统校验: 预约码存在 + 状态为已确认 + 是该商家的景点
     *   4. 标记为已使用
     */
    @Override
    @Transactional
    public BookingResponse verifyBooking(String bookingCode, Long verifierId, String role) {
        TourismBooking booking = bookingRepository.findByBookingCode(bookingCode.toUpperCase())
                .orElseThrow(() -> new BusinessException("预约码无效"));

        // 校验权限: 景点所有者或管理员
        if (!"ADMIN".equals(role)) {
            TourismSpot spot = spotRepository.findById(booking.getSpotId()).orElse(null);
            if (spot == null || !spot.getPublisherId().equals(verifierId)) {
                throw new BusinessException(403, "此预约不属于您的景点");
            }
        }

        if (booking.getStatus() != TourismBooking.Status.CONFIRMED) {
            throw new BusinessException("预约状态异常，当前: " + booking.getStatus()
                    + "（需要: CONFIRMED）");
        }

        booking.setStatus(TourismBooking.Status.USED);
        booking.setUsedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("预约已核验: bookingCode={}", bookingCode);

        return toBookingResponse(booking);
    }

    // ==================== 工具方法 ====================

    private TourismSpot findApprovedSpot(Long spotId) {
        TourismSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new BusinessException(404, "景点不存在"));
        if (spot.getStatus() != TourismSpot.Status.APPROVED) {
            throw new BusinessException("景点未通过审核，不能配置票型");
        }
        return spot;
    }

    private void checkSpotOwnerOrAdmin(TourismSpot spot, Long userId, String role) {
        if (!"ADMIN".equals(role) && !spot.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权操作此景点的票务");
        }
    }

    private TourismBooking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(404, "预约不存在"));
    }

    private TicketType.Category parseCategory(String category) {
        try {
            return TicketType.Category.valueOf(category.toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("无效的票型分类: " + category + "，可选: TICKET, ROOM");
        }
    }

    /** 生成6位预约码: 大写字母+数字 */
    private String generateBookingCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉容易混淆的 I/O/0/1
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        String code = sb.toString();
        // 极小概率重复，检查一下
        if (bookingRepository.findByBookingCode(code).isPresent()) {
            return generateBookingCode(); // 递归重试
        }
        return code;
    }

    private TicketTypeResponse toTicketResponse(TicketType ticket, LocalDate date) {
        Integer remaining = null;
        if (date != null) {
            if (ticket.getCategory() == TicketType.Category.ROOM) {
                int booked = bookingRepository.sumOverlappingRoomBookings(
                        ticket.getId(), date, date.plusDays(1));
                remaining = ticket.getDailyCapacity() - booked;
            } else {
                int booked = bookingRepository.sumBookedQuantityByTicketAndDate(ticket.getId(), date);
                remaining = ticket.getDailyCapacity() - booked;
            }
            remaining = Math.max(0, remaining);
        }

        return TicketTypeResponse.builder()
                .id(ticket.getId())
                .spotId(ticket.getSpotId())
                .name(ticket.getName())
                .description(ticket.getDescription())
                .price(ticket.getPrice())
                .category(ticket.getCategory().name())
                .dailyCapacity(ticket.getDailyCapacity())
                .minQuantity(ticket.getMinQuantity())
                .maxQuantity(ticket.getMaxQuantity())
                .advanceDays(ticket.getAdvanceDays())
                .enabled(ticket.getEnabled())
                .remaining(remaining)
                .build();
    }

    private BookingResponse toBookingResponse(TourismBooking booking) {
        String coverImage = spotRepository.findById(booking.getSpotId())
                .map(TourismSpot::getCoverImage).orElse(null);
        TicketType ticket = ticketRepository.findById(booking.getTicketId()).orElse(null);

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus().name())
                .spotId(booking.getSpotId())
                .spotName(booking.getSpotName())
                .spotCoverImage(coverImage)
                .ticketId(booking.getTicketId())
                .ticketName(booking.getTicketName())
                .ticketCategory(ticket != null ? ticket.getCategory().name() : null)
                .visitDate(booking.getVisitDate())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .quantity(booking.getQuantity())
                .nights(booking.getNights())
                .unitPrice(booking.getUnitPrice())
                .totalAmount(booking.getTotalAmount())
                .contactName(booking.getContactName())
                .contactPhone(booking.getContactPhone())
                .remark(booking.getRemark())
                .createdAt(booking.getCreatedAt())
                .paidAt(booking.getPaidAt())
                .usedAt(booking.getUsedAt())
                .cancelledAt(booking.getCancelledAt())
                .build();
    }
}
