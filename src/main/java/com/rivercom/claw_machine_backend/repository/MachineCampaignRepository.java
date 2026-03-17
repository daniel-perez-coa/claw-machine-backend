package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MachineCampaignRepository extends JpaRepository<MachineCampaign, Long> {
    Optional<MachineCampaign> findByStatus(MachineCampaignStatus status);
}
