package com.example.rural.repository;

import com.example.rural.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** 按订单号查询 */
    Optional<Order> findByOrderNo(String orderNo);

    /** 买家的订单列表（分页，按创建时间倒序） */
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    /** 买家按状态筛选订单 */
    Page<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Order.Status status, Pageable pageable);

    // ==================== 卖家收入统计（金融模块使用） ====================

    /** 卖家指定时间段内的已完成订单数（通过 OrderItem.sellerId 关联） */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN OrderItem oi ON o.id = oi.orderId " +
            "WHERE oi.sellerId = ?1 AND o.status = 'COMPLETED' " +
            "AND o.completedAt >= ?2 AND o.completedAt < ?3")
    long countCompletedBySeller(Long sellerId, LocalDateTime from, LocalDateTime to);

    /** 卖家指定时间段内的总收入 */
    @Query("SELECT COALESCE(SUM(oi.subtotal), 0) FROM OrderItem oi JOIN Order o ON oi.orderId = o.id " +
            "WHERE oi.sellerId = ?1 AND o.status = 'COMPLETED' " +
            "AND o.completedAt >= ?2 AND o.completedAt < ?3")
    BigDecimal sumRevenueBySeller(Long sellerId, LocalDateTime from, LocalDateTime to);

    /** 卖家历史总收入 */
    @Query("SELECT COALESCE(SUM(oi.subtotal), 0) FROM OrderItem oi JOIN Order o ON oi.orderId = o.id " +
            "WHERE oi.sellerId = ?1 AND o.status = 'COMPLETED'")
    BigDecimal sumTotalRevenueBySeller(Long sellerId);

    /** 卖家总完成订单数 */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN OrderItem oi ON o.id = oi.orderId " +
            "WHERE oi.sellerId = ?1 AND o.status = 'COMPLETED'")
    long countTotalCompletedBySeller(Long sellerId);

    /** 卖家总取消订单数 */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN OrderItem oi ON o.id = oi.orderId " +
            "WHERE oi.sellerId = ?1 AND o.status = 'CANCELLED'")
    long countTotalCancelledBySeller(Long sellerId);

    /** 卖家进行中订单总数（PAID + SHIPPED） */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN OrderItem oi ON o.id = oi.orderId " +
            "WHERE oi.sellerId = ?1 AND o.status IN ('PAID', 'SHIPPED')")
    long countActiveBySeller(Long sellerId);
}
