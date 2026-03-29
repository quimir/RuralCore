package com.example.rural.repository;

import com.example.rural.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** 查询用户的全部购物车（按添加时间倒序） */
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 查询用户购物车中某商品（唯一约束保证最多一条） */
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    /** 查询用户选中的购物车条目（用于下单结算） */
    List<CartItem> findByUserIdAndSelectedTrue(Long userId);

    /** 删除用户购物车中某商品 */
    void deleteByUserIdAndProductId(Long userId, Long productId);

    /** 清空用户购物车 */
    void deleteByUserId(Long userId);

    /** 统计用户购物车商品种类数（用于前端购物车角标） */
    long countByUserId(Long userId);
}
