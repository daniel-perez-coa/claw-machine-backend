package com.rivercom.claw_machine_backend.dto;

import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;

public record MachineCampaignUpdateRequestDTO(
   MachineCampaignStatus status,
   String notes
) {}
