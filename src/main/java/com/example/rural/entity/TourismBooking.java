package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅游预约实体
 *
 * 数据库表: tourism_booking
 *
 * 预约流程（对比商品订单的区别）:
 *
 *   商品订单:  加购物车 → 慢慢选 → 一起结算 → 发货 → 收货
 *   旅游预约:  选日期+票型 → 检查余量 → 立即预约 → 到场使用
 *
 * 预约状态流转:
 *
 *   PENDING_PAYMENT  ──→  CONFIRMED  ──→  USED  ──→  COMPLETED
 *        │                    │
 *        ↓                    ↓
 *     CANCELLED           CANCELLED（退款）
 *                          EXPIRED（过期未使用，自动完成）
 *
 * 核心字段说明:
 *   booking_code — 6位预约码（到场出示给商家核验）
 *   visit_date   — 门票类的游玩日期
 *   check_in / check_out — 住宿类的入住/退房日期
 *   quantity     — 预约数量（人次或房间数）
 *
 * 关联关系:
 *   tourism_booking.user_id   → user.id
 *   tourism_booking.spot_id   → tourism_spot.id
 *   tourism_booking.ticket_id → tourism_ticket.id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_booking", indexes = {
        @Index(name = "idx_tb_user", columnList = "user_id"),
        @Index(name = "idx_tb_spot", columnList = "spot_id"),
        @Index(name = "idx_tb_code", columnList = "booking_code"),
        @Index(name = "idx_tb_date", columnList = "visit_date"),
        @Index(name = "idx_tb_status", columnList = "status")
})
public class TourismBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 预约用户 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 旅游项目 */
    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    /** 票型 */
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    // ========== 快照信息（下单时冻结，不随后续修改而变） ==========

    /** 景点名称（快照） */
    @Column(name = "spot_name", nullable = false, length = 200)
    private String spotName;

    /** 票型名称（快照） */
    @Column(name = "ticket_name", nullable = false, length = 100)
    private String ticketName;

    /** 单价（快照） */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // ========== 预约信息 ==========

    /**
     * 游玩日期（门票/体验类使用）
     * 住宿类此字段为 null，使用 checkIn / checkOut
     */
    @Column(name = "visit_date")
    private LocalDate visitDate;

    /** 入住日期（住宿类） */
    @Column(name = "check_in")
    private LocalDate checkIn;

    /** 退房日期（住宿类） */
    @Column(name = "check_out")
    private LocalDate checkOut;

    /** 预约数量（人次 或 房间数） */
    @Column(nullable = false)
    private Integer quantity;

    /** 住宿晚数（住宿类自动计算: checkOut - checkIn） */
    @Column(nullable = false)
    @Builder.Default
    private Integer nights = 0;

    /** 总金额 = unitPrice × quantity × (nights > 0 ? nights : 1) */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 预约码（6位大写字母+数字）
     * 到场出示给商家，商家输入核验码确认「已使用」
     * 例: A3K7M2
     */
    @Column(name = "booking_code", nullable = false, unique = true, length = 10)
    private String bookingCode;

    /** 预约状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING_PAYMENT;

    // ========== 联系人信息 ==========

    @Column(name = "contact_name", nullable = false, length = 50)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    /** 支付时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 核验时间（商家确认使用） */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** 取消时间 */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING_PAYMENT,  // 待付款
        CONFIRMED,        // 已确认（已付款）
        USED,             // 已使用（商家核验）
        COMPLETED,        // 已完成
        CANCELLED,        // 已取消
        EXPIRED           // 已过期（超过游玩日期未使用）
    }
}
