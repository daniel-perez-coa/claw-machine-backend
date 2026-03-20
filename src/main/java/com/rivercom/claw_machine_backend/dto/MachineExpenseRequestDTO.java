package com.rivercom.claw_machine_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MachineExpenseRequestDTO(
        @NotNull Long prizeId,
        @NotNull @Positive Integer quantity
) {
}