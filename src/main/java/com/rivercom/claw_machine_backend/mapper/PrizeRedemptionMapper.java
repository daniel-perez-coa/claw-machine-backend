package com.rivercom.claw_machine_backend.mapper;

import com.rivercom.claw_machine_backend.domain.entity.*;
import com.rivercom.claw_machine_backend.dto.PrizeRedemptionResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PrizeRedemptionMapper {

    public PrizeRedemptionResponseDTO toResponse(PrizeRedemption prizeRedemption) {
        return new PrizeRedemptionResponseDTO(
                prizeRedemption.getUser().getName(),
                prizeRedemption.getUser().getPhone(),
                prizeRedemption.getPrize().getName(),
                prizeRedemption.getPointTransaction().getPreviousBalance(),
                prizeRedemption.getPointTransaction().getNewBalance(),
                prizeRedemption.getPointTransaction().getTransactionType().name(),
                prizeRedemption.getPointsSpent()
        );
    }
}
