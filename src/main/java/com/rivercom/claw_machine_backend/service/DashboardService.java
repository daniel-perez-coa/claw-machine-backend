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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final MachineCampaignRepository machineCampaignRepository;
    private final IncomeRecordsRepository incomeRecordsRepository;
    private final PrizeRedemptionsRepository prizeRedemptionsRepository;
    private final MachineExpenseRecordsRepository machineExpenseRecordsRepository;

    @Transactional
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
                machineExpenseRecordsRepository.findByCampaignIdAndRestocked(campaign.getId(), false);
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

        if (surplusAfterJackpot.compareTo(BigDecimal.ZERO) > 0 && !Boolean.TRUE.equals(campaign.getMajorPrizeAlertActive())) {
            campaign.setMajorPrizeAlertActive(true);
            machineCampaignRepository.save(campaign);
        }

        return new DashboardInformationDTO(
                incomeTotalMoney,
                remainingToJackpot,
                surplusAfterJackpot,
                incomeTotalMoney.multiply(new BigDecimal("0.40")),
                incomeTotalMoney.multiply(new BigDecimal("0.40")),
                incomeTotalMoney.multiply(new BigDecimal("0.20")),
                campaign.getMajorPrizeAlertActive(),
                alertDTOList
        );
    }

    private List<AlertDTO> getAlertDTOs(List<MachineExpenseRecords> machineExpenseRecordsNeedsToRestock) {
        Map<String, Integer> quantityByCategory = new LinkedHashMap<>();

        for (MachineExpenseRecords machineExpenseRecord : machineExpenseRecordsNeedsToRestock) {
            String categoryName = machineExpenseRecord.getPrize().getCategory().getName();
            quantityByCategory.merge(categoryName, machineExpenseRecord.getQuantity(), Integer::sum);
        }

        List<AlertDTO> alertDTOList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantityByCategory.entrySet()) {
            alertDTOList.add(new AlertDTO(
                    entry.getValue(),
                    String.format(
                            "%s necesitan rellenarse: %s vece(s).",
                            entry.getKey(),
                            entry.getValue()
                    )
            ));
        }

        return alertDTOList;
    }
}
