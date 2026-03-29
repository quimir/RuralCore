package com.example.rural.repository;

import com.example.rural.entity.TourismReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourismReviewRepository extends JpaRepository<TourismReview, Long> {

    /** 景点的可见评论（分页，最新在前） */
    Page<TourismReview> findBySpotIdAndVisibleTrueOrderByCreatedAtDesc(Long spotId, Pageable pageable);

    /** 查用户对某景点的评论（唯一约束保证最多一条） */
    Optional<TourismReview> findByUserIdAndSpotId(Long userId, Long spotId);

    /** 景点的平均评分 */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM TourismReview r WHERE r.spotId = ?1 AND r.visible = true")
    Double getAverageRating(Long spotId);

    /** 景点的可见评论数 */
    long countBySpotIdAndVisibleTrue(Long spotId);
}
