package com.rivercom.claw_machine_backend.dto;

import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record MachineCampaignResponse (
        Long id,
        String name,
        String prizeName,
        String prizeDescription,
        MachineCampaignStatus status,
        BigDecimal baseTargetAmount,
        String notes,
        LocalDateTime openedAt,
        LocalDateTime closedAt
){}
