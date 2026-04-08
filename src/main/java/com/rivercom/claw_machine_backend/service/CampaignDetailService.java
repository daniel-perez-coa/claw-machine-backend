package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.IncomeRecords;
import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import com.rivercom.claw_machine_backend.dto.MachineCampaignResponseDTO;
import com.rivercom.claw_machine_backend.dto.MachineCampaignUpdateRequestDTO;
import com.rivercom.claw_machine_backend.dto.NewMachineCampaignCampaignDTO;
import com.rivercom.claw_machine_backend.mapper.MachineCampaignMapper;
import com.rivercom.claw_machine_backend.repository.IncomeRecordsRepository;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import com.rivercom.claw_machine_backend.repository.MachineExpenseRecordsRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignDetailService {

    private final MachineCampaignRepository repository;
    private final PrizeRepository prizeRepository;
    private final MachineExpenseRecordsRepository machineExpenseRecordsRepository;
    private final IncomeRecordsRepository incomeRecordsRepository;
    private final MachineCampaignMapper mapper;

    public List<MachineCampaignResponseDTO> listAll() {
        List<MachineCampaign> machineCampaigns = repository.findAll().stream()
                .sorted(Comparator.comparing(
                        MachineCampaign::getOpenedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        return mapper.toResponseList(machineCampaigns, getTotalMoneyRaisedByCampaign(machineCampaigns));
    }

    @Transactional
    public MachineCampaignResponseDTO createCampaign(NewMachineCampaignCampaignDTO request) {
        Optional<Prize> prize = prizeRepository.findById(request.majorPrizeId());
        Optional<MachineCampaign> existingOpenCampaign =
                repository.findByStatus(MachineCampaignStatus.OPEN);

        if (existingOpenCampaign.isPresent()) {
            MachineCampaign openCampaign = existingOpenCampaign.get();
            log.warn(
                    "No se puede crear la campaña {} porque ya existe una campaña OPEN: {}",
                    request.name(),
                    openCampaign.getName()
            );
            return null;
        }

        if (prize.isEmpty()) {
            log.warn("El premio con el id {} no existe", request.majorPrizeId());
            return null;
        }

        MachineCampaign newCampaign = new MachineCampaign();
        newCampaign.setName(request.name());
        newCampaign.setMajorPrize(prize.get());
        newCampaign.setStatus(MachineCampaignStatus.OPEN);
        newCampaign.setBaseTargetAmount(request.baseTargetAmount());

        MachineCampaign savedCampaign = repository.save(newCampaign);
        return mapper.toResponse(savedCampaign, BigDecimal.ZERO);
    }

    @Transactional
    public MachineCampaignResponseDTO updateCampaign(MachineCampaignUpdateRequestDTO request, Long id) {
        Optional<MachineCampaign> existingCampaign = repository.findById(id);

        if (existingCampaign.isEmpty()) {
            log.warn("No se ha encontrado ninguna campaña con el ID {}", id);
            return null;
        }

        MachineCampaign campaign = existingCampaign.get();

        if (request.status() == MachineCampaignStatus.CLOSED) {
            campaign.setStatus(MachineCampaignStatus.CLOSED);
            campaign.setClosedAt(LocalDateTime.now());
            campaign.setNotes("Campaña cerrada exitosamente porque se logró la meta.");
            resolvePendingRestockForCampaign(campaign.getId());
        }

        if (request.status() == MachineCampaignStatus.CANCELLED) {
            campaign.setStatus(MachineCampaignStatus.CANCELLED);
            campaign.setNotes(request.notes());
            campaign.setClosedAt(LocalDateTime.now());
            resolvePendingRestockForCampaign(campaign.getId());
        }

        MachineCampaign updatedCampaign = repository.save(campaign);
        return mapper.toResponse(updatedCampaign, getTotalMoneyRaised(updatedCampaign.getId()));
    }

    private void resolvePendingRestockForCampaign(Long campaignId) {
        machineExpenseRecordsRepository.findByCampaignIdAndRestocked(campaignId, false)
                .forEach((record) -> record.setRestocked(true));
    }

    private Map<Long, BigDecimal> getTotalMoneyRaisedByCampaign(List<MachineCampaign> campaigns) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();

        for (MachineCampaign campaign : campaigns) {
            totals.put(campaign.getId(), getTotalMoneyRaised(campaign.getId()));
        }

        return totals;
    }

    private BigDecimal getTotalMoneyRaised(Long campaignId) {
        List<IncomeRecords> incomeRecords = incomeRecordsRepository.findByCampaignId(campaignId);
        BigDecimal total = BigDecimal.ZERO;

        for (IncomeRecords incomeRecord : incomeRecords) {
            total = total.add(incomeRecord.getAmount());
        }

        return total;
    }
}
