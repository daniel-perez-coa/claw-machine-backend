package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserAddPointsDTO(
        @NotBlank String phone,
        @NotNull @Positive Integer points
) {}
