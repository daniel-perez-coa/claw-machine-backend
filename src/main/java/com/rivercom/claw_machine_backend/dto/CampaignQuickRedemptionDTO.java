package com.rivercom.claw_machine_backend.dto;

import java.util.List;

public record CampaignQuickRedemptionDTO(
        String operationGroupId,
        Long campaignId,
        String campaignName,
        Integer totalQuantity,
        Integer totalCost,
        Boolean restocked,
        String registeredAt,
        List<Long> expenseIds,
        List<CampaignQuickRedemptionItemDTO> items
) {
}
