package com.rivercom.claw_machine_backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardInformationDTO(
        BigDecimal totalMoneyRaised,
        BigDecimal remainingToJackpot,
        BigDecimal surplusAfterJackpot,
        BigDecimal firstPartnerShare,
        BigDecimal secondPartnerShare,
        BigDecimal servicesShare,
        Boolean majorPrizeAlertActive,
        List<AlertDTO> alerts
) {
}
