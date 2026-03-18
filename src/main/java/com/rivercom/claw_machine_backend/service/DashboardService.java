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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final MachineCampaignRepository machineCampaignRepository;
    private final IncomeRecordsRepository incomeRecordsRepository;
    private final PrizeRedemptionsRepository prizeRedemptionsRepository;
    private final MachineExpenseRecordsRepository machineExpenseRecordsRepository;

    public DashboardInformationDTO getDashboardInformation() {
        log.debug("Buscando información sobre campañas con estado: OPEN");

        MachineCampaign openCampaign = machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("No hay campaña activa"));

        log.debug("Se ha encontrado una campaña activa");

        BigDecimal targetAmount = openCampaign.getBaseTargetAmount();
        BigDecimal incomeTotalMoney = BigDecimal.ZERO;
        BigDecimal prizeRedemptionTotal = BigDecimal.ZERO;
        BigDecimal machineExpenseTotal = BigDecimal.ZERO;

        List<IncomeRecords> incomeRecordsList =
                incomeRecordsRepository.findByCampaignId(openCampaign.getId());
        for (IncomeRecords incomeRecord : incomeRecordsList) {
            incomeTotalMoney = incomeTotalMoney.add(incomeRecord.getAmount());
        }

        List<PrizeRedemption> prizeRedemptionList =
                prizeRedemptionsRepository.findByCampaignId(openCampaign.getId());
        for (PrizeRedemption prizeRedemption : prizeRedemptionList) {
            prizeRedemptionTotal = prizeRedemptionTotal.add(prizeRedemption.getPrize().getCost());
        }

        List<MachineExpenseRecords> machineExpenseRecordsList =
                machineExpenseRecordsRepository.findByCampaignId(openCampaign.getId());
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
                            machineExpenseRecord.getPrize().getCategory(),
                            machineExpenseRecord.getQuantity()
                    )
            ));
        }

        return alertDTOList;
    }
}