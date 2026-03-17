package com.rivercom.claw_machine_backend.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class MachineCampaignResponse {
    String name;
    String prizeName;
    String prizeDescription;
    String status;
    BigDecimal baseTargetAmount;
    String notes;
    LocalDateTime openedAt;
    LocalDateTime closedAt;
}
