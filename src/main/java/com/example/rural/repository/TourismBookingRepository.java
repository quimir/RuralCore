package com.example.rural.repository;

import com.example.rural.entity.TourismBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TourismBookingRepository extends JpaRepository<TourismBooking, Long> {

    /** 按预约码查询 */
    Optional<TourismBooking> findByBookingCode(String bookingCode);

    /** 用户的预约列表 */
    Page<TourismBooking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 用户按状态筛选预约 */
    Page<TourismBooking> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, TourismBooking.Status status, Pageable pageable);

    /**
     * 查询某票型某天已预约的总数量（排除已取消的）
     * 用于计算剩余容量: dailyCapacity - 已预约数
     */
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM TourismBooking b " +
            "WHERE b.ticketId = ?1 AND b.visitDate = ?2 " +
            "AND b.status NOT IN ('CANCELLED', 'EXPIRED')")
    int sumBookedQuantityByTicketAndDate(Long ticketId, LocalDate visitDate);

    /**
     * 住宿类: 查询某票型在日期范围内每天已预约的最大数量
     * 住宿需要检查入住期间每一天的容量
     */
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM TourismBooking b " +
            "WHERE b.ticketId = ?1 " +
            "AND b.checkIn < ?3 AND b.checkOut > ?2 " +
            "AND b.status NOT IN ('CANCELLED', 'EXPIRED')")
    int sumOverlappingRoomBookings(Long ticketId, LocalDate checkIn, LocalDate checkOut);

    /** 景点的预约列表（商家视角） */
    Page<TourismBooking> findBySpotIdOrderByCreatedAtDesc(Long spotId, Pageable pageable);
}
