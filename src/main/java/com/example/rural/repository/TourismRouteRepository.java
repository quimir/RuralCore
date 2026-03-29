package com.example.rural.repository;

import com.example.rural.entity.TourismRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourismRouteRepository extends JpaRepository<TourismRoute, Long> {

    /** 我的路线 */
    Page<TourismRoute> findByCreatorIdOrderByUpdatedAtDesc(Long creatorId, Pageable pageable);

    /** 公开路线（推荐页） */
    Page<TourismRoute> findByIsPublicTrueOrderByCopyCountDescCreatedAtDesc(Pageable pageable);
}
