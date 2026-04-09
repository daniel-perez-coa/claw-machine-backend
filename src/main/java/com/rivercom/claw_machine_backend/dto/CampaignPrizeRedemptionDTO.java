package com.rivercom.claw_machine_backend.dto;

public record CampaignPrizeRedemptionDTO(
        Long redemptionId,
        Long campaignId,
        String campaignName,
        String userName,
        String userPhone,
        String prizeName,
        String prizeCategory,
        Integer pointsSpent,
        Integer previousPoints,
        Integer remainingPoints,
        String redeemedAt
) {
}
