package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record NewPrizeDTO(
        @NotNull String prizeCategory,
        @NotBlank String name,
        String description,
        @NotNull @Min(1) Integer pointsCost,
        @NotNull @DecimalMin("0.01") BigDecimal cost
        ) {
}
