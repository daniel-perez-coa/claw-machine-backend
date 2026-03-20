package com.rivercom.claw_machine_backend.dto;

import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;

import java.math.BigDecimal;

public record PrizeDTO(
        Long id,
        PrizeCategory prizeCategory,
        String name,
        String description,
        Integer pointsCost,
        BigDecimal cost
        ) {
}
