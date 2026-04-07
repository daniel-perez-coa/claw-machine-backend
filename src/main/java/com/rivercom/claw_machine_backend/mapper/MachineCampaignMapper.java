package com.rivercom.claw_machine_backend.mapper;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.dto.MachineCampaignResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MachineCampaignMapper {

    private static final DateTimeFormatter CAMPAIGN_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MachineCampaignResponseDTO toResponse
            (MachineCampaign entity) {

        if (entity == null) {
            return null;
        }

        return new MachineCampaignResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getMajorPrize() != null ? entity.getMajorPrize().getName() : null,
                entity.getMajorPrize() != null ? entity.getMajorPrize().getDescription() : null,
                entity.getStatus(),
                entity.getBaseTargetAmount(),
                entity.getNotes(),
                formatDate(entity.getOpenedAt()),
                formatDate(entity.getOpenedAt()),
                formatDate(entity.getClosedAt())
        );
    }

    public List<MachineCampaignResponseDTO> toResponseList
            (List<MachineCampaign> entityList) {

        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toResponse)
                .toList();
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }

        return value.format(CAMPAIGN_DATE_FORMATTER);
    }
}
