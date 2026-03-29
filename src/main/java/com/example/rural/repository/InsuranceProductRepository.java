package com.example.rural.repository;

import com.example.rural.entity.InsuranceProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {

    Page<InsuranceProduct> findByStatusOrderByCreatedAtDesc(InsuranceProduct.Status status, Pageable pageable);

    Page<InsuranceProduct> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    boolean existsByProviderIdAndName(Long providerId, String name);

    /** 更新时检查重名（排除自身ID） */
    boolean existsByProviderIdAndNameAndIdNot(Long providerId, String name, Long excludeId);
}
