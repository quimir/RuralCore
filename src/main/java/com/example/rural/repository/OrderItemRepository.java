package com.example.rural.repository;

import com.example.rural.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** 查询订单的所有明细 */
    List<OrderItem> findByOrderId(Long orderId);

    /** 查询某卖家的所有订单明细（卖家视角：「我卖出的」） */
    List<OrderItem> findBySellerIdOrderByIdDesc(Long sellerId);
}
