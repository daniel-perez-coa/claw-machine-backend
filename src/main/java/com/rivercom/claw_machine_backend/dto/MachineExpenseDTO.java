package com.rivercom.claw_machine_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MachineExpenseDTO(
        Long id,
        String campaignName,
        String majorPrizeName,
        String majorPrizeDescription,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        Boolean restocked,
        LocalDateTime registeredAt
) {
}