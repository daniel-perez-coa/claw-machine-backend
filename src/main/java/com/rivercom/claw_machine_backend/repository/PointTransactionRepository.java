package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import com.rivercom.claw_machine_backend.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "campaign"})
    Optional<PointTransaction> findById(Long id);

    @EntityGraph(attributePaths = {"user", "campaign"})
    List<PointTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType transactionType);

    @EntityGraph(attributePaths = {"user", "campaign"})
    List<PointTransaction> findByTransactionTypeAndCampaignIdOrderByCreatedAtDesc(
            TransactionType transactionType,
            Long campaignId
    );

    @EntityGraph(attributePaths = {"user", "campaign"})
    List<PointTransaction> findByTransactionTypeAndCampaignIsNullOrderByCreatedAtDesc(TransactionType transactionType);
}
