package com.example.rural.repository;

import com.example.rural.entity.TourismFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourismFavoriteRepository extends JpaRepository<TourismFavorite, Long> {

    /** 查用户是否已收藏某景点 */
    Optional<TourismFavorite> findByUserIdAndSpotId(Long userId, Long spotId);

    /** 用户是否已收藏 */
    boolean existsByUserIdAndSpotId(Long userId, Long spotId);

    /** 用户的收藏列表（分页，最新在前） */
    Page<TourismFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 景点的收藏数 */
    long countBySpotId(Long spotId);
}
