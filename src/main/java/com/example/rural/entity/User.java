package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类 — 对应数据库 user 表
 *
 * JPA 会根据此类自动创建/更新表结构（ddl-auto: update）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_phone", columnList = "phone")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（唯一，用于登录） */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 密码（BCrypt 加密存储） */
    @Column(nullable = false, length = 255)
    private String password;

    /** 昵称 */
    @Column(length = 50)
    private String nickname;

    /** 手机号 */
    @Column(length = 20)
    private String phone;

    /** 邮箱 */
    @Column(length = 100)
    private String email;

    /** 头像 URL */
    @Column(length = 255)
    private String avatar;

    /**
     * 用户角色
     * ADMIN               - 系统管理员
     * FARMER              - 农户
     * MERCHANT            - 商家
     * TOURIST             - 游客（默认）
     * FINANCIAL_PROVIDER  - 金融服务商（银行/小贷公司）
     * INSURANCE_PROVIDER  - 保险提供商
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Role role = Role.TOURIST;

    /** 账号状态: 0-禁用 1-正常 */
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;

    /**
     * 机构名称（金融服务商/保险提供商专用）
     * 如: "农村信用社牟平支行"、"中国人保财险"
     */
    @Column(name = "org_name", length = 200)
    private String orgName;

    /**
     * 经营许可编号（金融服务商/保险提供商专用，管理员审核依据）
     */
    @Column(name = "license_no", length = 100)
    private String licenseNo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 生命周期回调 ====================

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 角色枚举 ====================

    public enum Role {
        ADMIN, FARMER, MERCHANT, TOURIST,
        FINANCIAL_PROVIDER,   // 金融服务商（银行、小贷公司）
        INSURANCE_PROVIDER    // 保险提供商
    }
}
