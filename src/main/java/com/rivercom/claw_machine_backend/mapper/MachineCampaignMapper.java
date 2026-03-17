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

       return MachineCampaignResponse.builder()
               .name(entity.getName())
               .prizeName(entity.getMajorPrize().getName())
               .prizeDescription(entity.getMajorPrize().getDescription())
               .status(String.valueOf(entity.getStatus()))
               .baseTargetAmount(entity.getBaseTargetAmount())
               .notes(entity.getNotes())
               .openedAt(entity.getOpenedAt())
               .closedAt(entity.getClosedAt() != null ? entity.getClosedAt() : null)
               .build();
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
