package com.rivercom.claw_machine_backend.mapper;

import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import com.rivercom.claw_machine_backend.dto.MachineExpenseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MachineExpenseRecordsMapper {

    public MachineExpenseDTO toDTO(MachineExpenseRecords entity) {
        return new MachineExpenseDTO(
                entity.getId(),
                entity.getCampaign().getName(),
                entity.getCampaign().getMajorPrize().getName(),
                entity.getCampaign().getMajorPrize().getDescription(),
                entity.getQuantity(),
                entity.getUnitCost(),
                entity.getTotalCost(),
                entity.getRestocked(),
                entity.getRegisteredAt()
        );
    }

    public List<MachineExpenseDTO> toDTOList(List<MachineExpenseRecords> list) {
        return list.stream()
                .map(this::toDTO)
                .toList();
    }
}