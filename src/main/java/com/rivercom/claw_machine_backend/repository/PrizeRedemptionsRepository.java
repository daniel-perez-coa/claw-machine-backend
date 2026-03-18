package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.PrizeRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrizeRedemptionsRepository extends JpaRepository<PrizeRedemption, Long> {
    List<PrizeRedemption> findByCampaignId(Long campaignId);
}
