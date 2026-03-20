package com.rivercom.claw_machine_backend.dto;

public record PrizeRedemptionResponseDTO(
        String userName,
        String phone,
        String prizeCategory,
        Integer previousBalance,
        Integer newBalance,
        String transactionType,
        Integer pointsSpent
) {
}
