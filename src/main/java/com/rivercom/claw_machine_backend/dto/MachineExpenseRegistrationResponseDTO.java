package com.rivercom.claw_machine_backend.dto;

import java.util.List;

public record MachineExpenseRegistrationResponseDTO(
        String message,
        List<Long> expenseIds
) {
}
