package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（订单头）
 *
 * 数据库表: orders（不用 order，是 SQL 保留字）
 *
 * 订单状态流转：
 *
 *   PENDING_PAYMENT  ──→  PAID  ──→  SHIPPED  ──→  COMPLETED
 *        │                  │
 *        ↓                  ↓
 *     CANCELLED          CANCELLED（退款）
 *
 * 关联关系：
 *   orders.buyer_id → user.id       （买家）
 *   order_item.order_id → orders.id （订单明细，一对多）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_buyer", columnList = "buyer_id"),
        @Index(name = "idx_order_no", columnList = "order_no"),
        @Index(name = "idx_order_status", columnList = "status")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单编号（展示给用户看的，格式: yyyyMMddHHmmss + 6位随机数）
     * 例: 20260211143052839271
     */
    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    /** 买家ID */
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    /** 订单总金额 */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** 订单状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING_PAYMENT;

    /** 收货地址簿ID（下单时引用的地址簿记录，可为null表示手动输入） */
    @Column(name = "address_id")
    private Long addressId;

    /** 收货地址（快照: 下单时冻结，后续地址簿修改不影响已有订单） */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /** 收货人姓名 */
    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    /** 收货人电话 */
    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    /** 买家备注 */
    @Column(length = 500)
    private String remark;

    /** 支付时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 发货时间 */
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
        PAID,             // 已付款（待发货）
        SHIPPED,          // 已发货（待收货）
        COMPLETED,        // 已完成
        CANCELLED         // 已取消
    }
}
