package com.rivercom.claw_machine_backend.dto;

public record CampaignAddPointsTransactionDTO(
        Long transactionId,
        Long campaignId,
        String campaignName,
        String userName,
        String userPhone,
        Integer pointsAdded,
        Integer previousBalance,
        Integer newBalance,
        String createdAt
) {
}
