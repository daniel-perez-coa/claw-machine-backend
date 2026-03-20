package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRedemptionRequestDTO(
        @NotBlank String phone,
        @NotBlank Long prizeId
) {
}
