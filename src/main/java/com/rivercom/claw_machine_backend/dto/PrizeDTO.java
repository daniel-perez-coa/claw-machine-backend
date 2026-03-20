package com.rivercom.claw_machine_backend.dto;

import java.math.BigDecimal;

public record PrizeDTO(
        Long id,
        String prizeCode,
        String prizeCategory,
        String name,
        String description,
        Integer pointsCost,
        BigDecimal cost
        ) {
}
