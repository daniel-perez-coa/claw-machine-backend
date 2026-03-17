package com.rivercom.claw_machine_backend.mapper;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.dto.MachineCampaignResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MachineCampaignMapper {

    public MachineCampaignResponse toResponse
            (MachineCampaign entity) {

        if (entity == null) {
            return null;
        }

        return new MachineCampaignResponse(
                entity.getId(),
                entity.getName(),
                entity.getMajorPrize() != null ? entity.getMajorPrize().getName() : null,
                entity.getMajorPrize() != null ? entity.getMajorPrize().getDescription() : null,
                entity.getStatus(),
                entity.getBaseTargetAmount(),
                entity.getNotes(),
                entity.getOpenedAt() != null ? entity.getClosedAt() : null,
                entity.getClosedAt()
        );
    }

    public List<MachineCampaignResponse> toResponseList
            (List<MachineCampaign> entityList) {

        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toResponse)
                .toList();
    }
}
