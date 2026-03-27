package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.IncomeRecords;
import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import com.rivercom.claw_machine_backend.domain.entity.PrizeRedemption;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import com.rivercom.claw_machine_backend.dto.AlertDTO;
import com.rivercom.claw_machine_backend.dto.DashboardInformationDTO;
import com.rivercom.claw_machine_backend.repository.IncomeRecordsRepository;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import com.rivercom.claw_machine_backend.repository.MachineExpenseRecordsRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRedemptionsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final MachineCampaignRepository machineCampaignRepository;
    private final IncomeRecordsRepository incomeRecordsRepository;
    private final PrizeRedemptionsRepository prizeRedemptionsRepository;
    private final MachineExpenseRecordsRepository machineExpenseRecordsRepository;

    @Transactional(readOnly = true)
    public DashboardInformationDTO getDashboardInformation() {
        log.debug("Buscando información sobre campañas con estado: OPEN");

        Optional<MachineCampaign> openCampaign =
                machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN);

        if (openCampaign.isEmpty()) {
            return null;
        }

        MachineCampaign campaign = openCampaign.get();

        log.debug("Se ha encontrado una campaña activa");

        BigDecimal targetAmount = campaign.getBaseTargetAmount();
        BigDecimal incomeTotalMoney = BigDecimal.ZERO;
        BigDecimal prizeRedemptionTotal = BigDecimal.ZERO;
        BigDecimal machineExpenseTotal = BigDecimal.ZERO;

        List<IncomeRecords> incomeRecordsList =
                incomeRecordsRepository.findByCampaignId(campaign.getId());
        for (IncomeRecords incomeRecord : incomeRecordsList) {
            incomeTotalMoney = incomeTotalMoney.add(incomeRecord.getAmount());
        }

        List<PrizeRedemption> prizeRedemptionList =
                prizeRedemptionsRepository.findByCampaignId(campaign.getId());
        for (PrizeRedemption prizeRedemption : prizeRedemptionList) {
            prizeRedemptionTotal = prizeRedemptionTotal.add(prizeRedemption.getPrize().getCost());
        }

        List<MachineExpenseRecords> machineExpenseRecordsList =
                machineExpenseRecordsRepository.findByCampaignId(campaign.getId());
        for (MachineExpenseRecords machineExpenseRecord : machineExpenseRecordsList) {
            machineExpenseTotal = machineExpenseTotal.add(machineExpenseRecord.getTotalCost());
        }

        List<MachineExpenseRecords> machineExpenseRecordsNeedsToRestock =
                machineExpenseRecordsRepository.findByRestocked(false);
        List<AlertDTO> alertDTOList = getAlertDTOs(machineExpenseRecordsNeedsToRestock);

        BigDecimal totalExpenses = prizeRedemptionTotal.add(machineExpenseTotal);

        BigDecimal jackpotBalance = targetAmount
                .subtract(incomeTotalMoney)
                .add(totalExpenses);

        BigDecimal remainingToJackpot = BigDecimal.ZERO;
        BigDecimal surplusAfterJackpot = BigDecimal.ZERO;

        if (jackpotBalance.compareTo(BigDecimal.ZERO) > 0) {
            remainingToJackpot = jackpotBalance;
        } else {
            surplusAfterJackpot = jackpotBalance.abs();
        }

        return new DashboardInformationDTO(
                incomeTotalMoney,
                remainingToJackpot,
                surplusAfterJackpot,
                incomeTotalMoney.multiply(new BigDecimal("0.40")),
                incomeTotalMoney.multiply(new BigDecimal("0.40")),
                incomeTotalMoney.multiply(new BigDecimal("0.20")),
                alertDTOList
        );
    }

    private List<AlertDTO> getAlertDTOs(List<MachineExpenseRecords> machineExpenseRecordsNeedsToRestock) {
        List<AlertDTO> alertDTOList = new ArrayList<>();

        for (MachineExpenseRecords machineExpenseRecord : machineExpenseRecordsNeedsToRestock) {
            alertDTOList.add(new AlertDTO(
                    machineExpenseRecord.getQuantity(),
                    String.format(
                            "Los premios con categoría: %s necesitan rellenarse: %s",
                            machineExpenseRecord.getPrize().getCategory().getName(),
                            machineExpenseRecord.getQuantity()
                    )
            ));
        }

        return alertDTOList;
    }
}