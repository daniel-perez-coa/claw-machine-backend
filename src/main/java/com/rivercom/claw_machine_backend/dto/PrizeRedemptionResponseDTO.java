package com.rivercom.claw_machine_backend.dto;

public record PrizeRedemptionResponseDTO(
        String userName,
        String userPhone,
        String prizeName,
        Integer previousPoints,
        Integer remainingPoints,
        String transactionType,
        Integer pointsSpent
) {
}
