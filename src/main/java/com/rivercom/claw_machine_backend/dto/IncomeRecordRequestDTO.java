package com.rivercom.claw_machine_backend.dto;

import java.math.BigDecimal;

public record IncomeRecordRequestDTO(
        BigDecimal amount,
        String notes
) {
}
