package com.example.rural.repository;

import com.example.rural.entity.InsurancePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    Page<InsurancePolicy> findByHolderIdOrderByCreatedAtDesc(Long holderId, Pageable pageable);

    Page<InsurancePolicy> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    /** 农户的生效中保单总保额 */
    @Query("SELECT COALESCE(SUM(p.totalCoverage), 0) FROM InsurancePolicy p " +
            "WHERE p.holderId = ?1 AND p.status = 'ACTIVE'")
    BigDecimal sumActiveCoverageByHolder(Long holderId);

    /** 农户的生效中保单总保费 */
    @Query("SELECT COALESCE(SUM(p.totalPremium), 0) FROM InsurancePolicy p " +
            "WHERE p.holderId = ?1 AND p.status = 'ACTIVE'")
    BigDecimal sumActivePremiumByHolder(Long holderId);

    long countByHolderIdAndStatus(Long holderId, InsurancePolicy.Status status);
}
