package com.rivercom.claw_machine_backend.dto;

import java.util.List;

public record MachineExpenseFormDTO(
        List<MachineExpenseRequestDTO>items
) {
}
