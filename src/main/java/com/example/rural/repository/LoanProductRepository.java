package com.example.rural.repository;

import com.example.rural.entity.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    Page<LoanProduct> findByStatusOrderByCreatedAtDesc(LoanProduct.Status status, Pageable pageable);

    Page<LoanProduct> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    boolean existsByProviderIdAndName(Long providerId, String name);

    /** 更新时检查重名（排除自身ID） */
    boolean existsByProviderIdAndNameAndIdNot(Long providerId, String name, Long excludeId);
}
