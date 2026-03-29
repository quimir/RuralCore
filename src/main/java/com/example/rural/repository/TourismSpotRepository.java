package com.example.rural.repository;

import com.example.rural.entity.TourismSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourismSpotRepository extends JpaRepository<TourismSpot, Long>,
        JpaSpecificationExecutor<TourismSpot> {

    /** 发布者的所有旅游项目（按创建时间倒序） */
    List<TourismSpot> findByPublisherIdOrderByCreatedAtDesc(Long publisherId);

    /** 统计某状态的数量（管理后台统计用） */
    long countByStatus(TourismSpot.Status status);

    /**
     * 查询同一发布者是否已有同名项目（排除指定状态，如已取消的）
     * 用于发布和编辑时的重复检测
     */
    boolean existsByPublisherIdAndTitleAndStatusNot(Long publisherId, String title, TourismSpot.Status status);

    /**
     * 查询同一发布者是否已有同名项目（编辑时排除自身）
     */
    boolean existsByPublisherIdAndTitleAndIdNotAndStatusNot(Long publisherId, String title, Long excludeId, TourismSpot.Status status);

    /**
     * 全局查询是否存在同名+同地址的项目（不同发布者也算重复）
     * 防止不同商家发布同一个景点
     */
    boolean existsByTitleAndAddressAndStatusNot(String title, String address, TourismSpot.Status status);

    /**
     * 全局查询（编辑时排除自身）
     */
    boolean existsByTitleAndAddressAndIdNotAndStatusNot(String title, String address, Long excludeId, TourismSpot.Status status);
}
