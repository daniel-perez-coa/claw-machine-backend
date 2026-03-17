package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineCampaignRepository extends JpaRepository<MachineCampaign, Long> {
}
