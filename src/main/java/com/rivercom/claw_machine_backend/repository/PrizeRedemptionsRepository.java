package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.PrizeRedemption;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrizeRedemptionsRepository extends JpaRepository<PrizeRedemption, Long> {

    @EntityGraph(attributePaths = {"prize", "prize.category"})
    List<PrizeRedemption> findByCampaignId(Long campaignId);

    @EntityGraph(attributePaths = {"user", "prize", "prize.category", "pointTransaction", "campaign"})
    List<PrizeRedemption> findByCampaignIdOrderByRedeemedAtDesc(Long campaignId);

    @Override
    @EntityGraph(attributePaths = {"user", "prize", "prize.category", "pointTransaction", "campaign"})
    Optional<PrizeRedemption> findById(Long id);
}
