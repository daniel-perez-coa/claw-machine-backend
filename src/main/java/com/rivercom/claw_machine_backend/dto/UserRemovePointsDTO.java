package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserRemovePointsDTO(
        @NotBlank String phone,
        @NotNull @Positive Integer points,
        @NotBlank String notes
) {}
