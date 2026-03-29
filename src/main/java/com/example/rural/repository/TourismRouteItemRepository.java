package com.example.rural.repository;

import com.example.rural.entity.TourismRouteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourismRouteItemRepository extends JpaRepository<TourismRouteItem, Long> {

    /** 路线的所有行程项（按天→排序） */
    List<TourismRouteItem> findByRouteIdOrderByDayNumberAscSortOrderAsc(Long routeId);

    /** 删除路线下所有行程项 */
    void deleteByRouteId(Long routeId);

    /** 是否已经包含某景点 */
    boolean existsByRouteIdAndSpotId(Long routeId, Long spotId);
}
