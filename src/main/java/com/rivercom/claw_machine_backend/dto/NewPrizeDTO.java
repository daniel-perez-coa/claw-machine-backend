package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record NewPrizeDTO(
        @NotNull String prizeCategory,
        @NotBlank String name,
        String description,
        @NotNull Integer pointsCost,
        @NotNull BigDecimal cost
        ) {
}
