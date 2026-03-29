package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票务/房型配置实体
 *
 * 数据库表: tourism_ticket
 *
 * 景点发布者配置自己的可预约选项:
 *
 *   采摘园:
 *     - 成人票  ¥68/人   每日限100人
 *     - 儿童票  ¥38/人   每日限100人
 *     - 家庭套票 ¥158/组  每日限30组
 *
 *   民宿:
 *     - 标准间  ¥298/晚  共10间
 *     - 家庭套房 ¥498/晚  共5间
 *     - 星空房  ¥698/晚  共2间
 *
 *   农事体验:
 *     - 插秧体验 ¥88/人   每日限20人（需至少2人成团）
 *
 * 票型分类:
 *   TICKET — 门票类（按人/组，当天有效）
 *   ROOM   — 住宿类（按间/晚，有入住和退房日期）
 *
 * 关联关系:
 *   tourism_ticket.spot_id → tourism_spot.id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tourism_ticket", indexes = {
        @Index(name = "idx_tt_spot", columnList = "spot_id")
})
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属旅游项目 */
    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    /** 票型名称，如 "成人票"、"标准间" */
    @Column(nullable = false, length = 100)
    private String name;

    /** 票型描述 */
    @Column(length = 500)
    private String description;

    /** 单价 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 票型分类
     *   TICKET — 门票/体验（按人次，指定游玩日期）
     *   ROOM   — 住宿（按间/晚，有入住日和离店日）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Category category;

    /** 每日可预约数量（门票=人次，住宿=房间数） */
    @Column(name = "daily_capacity", nullable = false)
    private Integer dailyCapacity;

    /** 最少预约数量（如 "至少2人成团"） */
    @Column(name = "min_quantity")
    @Builder.Default
    private Integer minQuantity = 1;

    /** 单次最多预约数量 */
    @Column(name = "max_quantity")
    @Builder.Default
    private Integer maxQuantity = 10;

    /** 需提前多少天预约（0=当天可约） */
    @Column(name = "advance_days")
    @Builder.Default
    private Integer advanceDays = 0;

    /** 是否上架（发布者可临时关闭某个票型） */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

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

    public enum Category {
        TICKET,  // 门票/体验
        ROOM     // 住宿
    }
}
