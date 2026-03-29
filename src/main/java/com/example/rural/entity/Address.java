package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收货地址实体
 *
 * 每个用户可有多个收货地址，其中最多一个为默认地址。
 *
 * 下单时:
 *   方式1: 传 addressId → 后端自动查地址填入订单
 *   方式2: 传 shippingAddress + receiverName + receiverPhone → 兼容旧方式
 *
 * fullAddress 由后端拼接: province + city + district + detail
 * 前端也可自行拼接展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "address", indexes = {
        @Index(name = "idx_address_user", columnList = "user_id")
})
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 收货人姓名 */
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    /** 收货人手机 */
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    /** 省 */
    @Column(nullable = false, length = 50)
    private String province;

    /** 市 */
    @Column(nullable = false, length = 50)
    private String city;

    /** 区/县 */
    @Column(nullable = false, length = 50)
    private String district;

    /** 详细地址（街道门牌号等） */
    @Column(nullable = false, length = 200)
    private String detail;

    /**
     * 完整地址（后端自动拼接: 省+市+区+详细地址）
     * 存储冗余字段是为了订单快照时直接复制，无需再次拼接
     */
    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    /** 是否默认地址（每个用户最多一个） */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        buildFullAddress();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        buildFullAddress();
    }

    /** 拼接完整地址 */
    private void buildFullAddress() {
        this.fullAddress = (province != null ? province : "")
                + (city != null ? city : "")
                + (district != null ? district : "")
                + (detail != null ? detail : "");
    }
}
