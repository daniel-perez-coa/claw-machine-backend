package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MachineCampaignNewRequest(
        @NotBlank String name,
        @NotNull Long majorPrizeId,
        @NotNull @DecimalMin("0.01") BigDecimal baseTargetAmount
) {}
