package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.IncomeRecords;
import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import com.rivercom.claw_machine_backend.dto.IncomeRecordRequestDTO;
import com.rivercom.claw_machine_backend.repository.IncomeRecordsRepository;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeRecordService {

    private final IncomeRecordsRepository repository;
    private final MachineCampaignRepository machineCampaignRepository;

    public void incomeRecords (IncomeRecordRequestDTO request) {
        IncomeRecords incomeRecords = new IncomeRecords();
        MachineCampaign openCampaign = machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("No hay campaña activa"));

        log.debug("Se ha encontrado una campaña activa");

        incomeRecords.setCampaign(openCampaign);
        incomeRecords.setAmount(request.amount());
        incomeRecords.setNotes(request.notes());
        repository.save(incomeRecords);
    }
}
