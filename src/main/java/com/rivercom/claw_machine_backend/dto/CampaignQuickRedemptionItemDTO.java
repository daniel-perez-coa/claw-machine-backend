package com.rivercom.claw_machine_backend.dto;

public record CampaignQuickRedemptionItemDTO(
        Long expenseId,
        String prizeName,
        String prizeCategory,
        Integer quantity,
        Integer unitCost,
        Integer totalCost,
        Boolean restocked
) {
}
