package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record NewUserDTO(
        @NotBlank String name,
        @NotBlank String phone) {
}
