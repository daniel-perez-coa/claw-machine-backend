package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import com.rivercom.claw_machine_backend.dto.MachineExpenseDTO;
import com.rivercom.claw_machine_backend.dto.MachineExpenseRequestDTO;
import com.rivercom.claw_machine_backend.mapper.MachineExpenseRecordsMapper;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import com.rivercom.claw_machine_backend.repository.MachineExpenseRecordsRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MachineExpenseRecordsService {

    private final MachineExpenseRecordsRepository repository;
    private final MachineCampaignRepository machineCampaignRepository;
    private final PrizeRepository prizeRepository;

    private final MachineExpenseRecordsMapper mapper;

    @Transactional
    public void registerExpenses(List<MachineExpenseRequestDTO> requests) {
        MachineCampaign openCampaign = machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No hay campaña abierta"));

        for (MachineExpenseRequestDTO request : requests) {
            Prize prize = prizeRepository.findById(request.prizeId())
                    .orElseThrow(() -> new IllegalArgumentException("El premio no existe"));

            if (!Boolean.TRUE.equals(prize.getIsActive())) {
                throw new IllegalArgumentException("El premio no está activo");
            }

            BigDecimal unitCost = prize.getCost();
            BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(request.quantity()));

            MachineExpenseRecords expenseRecord = new MachineExpenseRecords();
            expenseRecord.setCampaign(openCampaign);
            expenseRecord.setPrize(prize);
            expenseRecord.setQuantity(request.quantity());
            expenseRecord.setUnitCost(unitCost);
            expenseRecord.setTotalCost(totalCost);
            expenseRecord.setRestocked(false);

            repository.save(expenseRecord);
        }
    }

    @Transactional
    public MachineExpenseDTO updateExpense(Long id, MachineExpenseRequestDTO request) {
        log.debug("Actualizando gasto/canje rápido id={}", id);

        MachineExpenseRecords existingRecord = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El registro no existe"));

        Prize prize = prizeRepository.findById(request.prizeId())
                .orElseThrow(() -> new IllegalArgumentException("El premio no existe"));

        if (!Boolean.TRUE.equals(prize.getIsActive())) {
            throw new IllegalArgumentException("El premio no está activo");
        }

        BigDecimal unitCost = prize.getCost();
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(request.quantity()));

        existingRecord.setPrize(prize);
        existingRecord.setQuantity(request.quantity());
        existingRecord.setUnitCost(unitCost);
        existingRecord.setTotalCost(totalCost);

        repository.save(existingRecord);

        log.debug("Registro actualizado correctamente id={}", id);
        return mapper.toDTO(existingRecord);
    }

    @Transactional
    public void markAsRestocked(Long id) {
        log.debug("Marcando registro como resurtido id={}", id);

        MachineExpenseRecords existingRecord = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El registro no existe"));

        existingRecord.setRestocked(true);
        repository.save(existingRecord);

        log.debug("Registro marcado como resurtido id={}", id);
    }

    public List<MachineExpenseDTO> findAll() {
        return mapper.toDTOList(repository.findAll());
    }

    public List<MachineExpenseDTO> findOpenCampaignExpenses() {
        MachineCampaign openCampaign = machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No hay campaña abierta"));

        return mapper.toDTOList(repository.findByCampaignId(openCampaign.getId()));
    }

    public List<MachineExpenseDTO> findPendingRestock() {
        MachineCampaign openCampaign = machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No hay campaña abierta"));

        return mapper.toDTOList(repository.findByCampaignIdAndRestocked(openCampaign.getId(), false));
    }
}
