package com.rivercom.claw_machine_backend.dto;

import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;

import java.math.BigDecimal;

public record MachineCampaignResponseDTO(
        Long id,
        String name,
        String prizeName,
        String prizeDescription,
        MachineCampaignStatus status,
        BigDecimal baseTargetAmount,
        String notes,
        String createdAt,
        String openedAt,
        String closedAt
){}
