package com.example.rural.repository;

import com.example.rural.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 *
 * JpaRepository         → 基础 CRUD
 * JpaSpecificationExecutor → 动态条件查询（管理员用户列表筛选）
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>,
                                        JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     * 自动生成: SELECT * FROM user WHERE username = ?
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据手机号查找用户
     * 自动生成: SELECT * FROM user WHERE phone = ?
     */
    Optional<User> findByPhone(String phone);

    /**
     * 判断用户名是否已存在
     * 自动生成: SELECT COUNT(*) > 0 FROM user WHERE username = ?
     */
    boolean existsByUsername(String username);

    /**
     * 判断手机号是否已被注册
     */
    boolean existsByPhone(String phone);

    /**
     * 判断邮箱是否已被注册
     */
    boolean existsByEmail(String email);
}
