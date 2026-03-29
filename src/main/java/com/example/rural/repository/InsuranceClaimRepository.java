package com.example.rural.repository;

import com.example.rural.entity.InsuranceClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    Page<InsuranceClaim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId, Pageable pageable);

    Page<InsuranceClaim> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    Page<InsuranceClaim> findByProviderIdAndStatusOrderByCreatedAtDesc(
            Long providerId, InsuranceClaim.Status status, Pageable pageable);

    boolean existsByPolicyIdAndStatusIn(Long policyId, java.util.Collection<InsuranceClaim.Status> statuses);
}
