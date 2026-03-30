package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRedemptionRequestDTO(
        @NotBlank String phone,
        @NotNull Long prizeId
) {
}
