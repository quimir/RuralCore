package com.example.rural.repository;

import com.example.rural.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Page<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

    Page<LoanApplication> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    Page<LoanApplication> findByProviderIdAndStatusOrderByCreatedAtDesc(
            Long providerId, LoanApplication.Status status, Pageable pageable);

    /** 农户是否对同一贷款产品有进行中的申请 */
    boolean existsByApplicantIdAndProductIdAndStatusIn(
            Long applicantId, Long productId, java.util.Collection<LoanApplication.Status> statuses);
}
