-- ============================================================
-- 乡村振兴管理系统 - 数据库初始化脚本
-- 执行方式: mysql -u root -p < init.sql
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `rural_revitalization`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `rural_revitalization`;

-- 2. 用户表（JPA 会根据 Entity 自动创建，此脚本作为参考和备份）
--    如果使用 ddl-auto: update，JPA 会自动建表，无需手动执行以下 SQL
--    如果使用 ddl-auto: none（生产环境），则需要手动执行

CREATE TABLE IF NOT EXISTS `user` (
                                      `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `username`   VARCHAR(50)  NOT NULL UNIQUE     COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL            COMMENT '密码(BCrypt加密)',
    `nickname`   VARCHAR(50)                      COMMENT '昵称',
    `phone`      VARCHAR(20)                      COMMENT '手机号',
    `email`      VARCHAR(100)                     COMMENT '邮箱',
    `avatar`     VARCHAR(255)                     COMMENT '头像URL',
    `role`       VARCHAR(20)  NOT NULL DEFAULT 'TOURIST' COMMENT '角色: ADMIN/FARMER/MERCHANT/TOURIST',
    `status`     INT          NOT NULL DEFAULT 1  COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME                         COMMENT '创建时间',
    `updated_at` DATETIME                         COMMENT '更新时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_phone` (`phone`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 插入默认管理员账号（密码为 admin123 的 BCrypt 哈希值）
--    如需使用，取消下面的注释
-- INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`, `created_at`, `updated_at`)
-- VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN', 1, NOW(), NOW());

