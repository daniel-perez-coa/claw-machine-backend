package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.entity.PrizeRedemption;
import com.rivercom.claw_machine_backend.domain.entity.User;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import com.rivercom.claw_machine_backend.domain.enums.TransactionType;
import com.rivercom.claw_machine_backend.dto.PrizeRedemptionResponseDTO;
import com.rivercom.claw_machine_backend.dto.UserRedemptionRequestDTO;
import com.rivercom.claw_machine_backend.mapper.PrizeRedemptionMapper;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import com.rivercom.claw_machine_backend.repository.PointTransactionRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRedemptionsRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import com.rivercom.claw_machine_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrizeRedemptionService {

    private final PrizeRedemptionsRepository repository;
    private final UserRepository userRepository;
    private final PrizeRepository prizeRepository;
    private final MachineCampaignRepository machineCampaignRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PrizeRedemptionMapper mapper;

    @Transactional
    public PrizeRedemptionResponseDTO spentPoints(UserRedemptionRequestDTO request) {

        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        Prize prize = prizeRepository.findById(request.prizeId())
                .orElseThrow(() -> new IllegalArgumentException("El premio no existe"));

        MachineCampaign campaign = machineCampaignRepository.findByStatus(MachineCampaignStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No hay campanas abiertas"));

        if (!Boolean.TRUE.equals(prize.getIsActive())) {
            throw new IllegalArgumentException("El premio no esta activo");
        }

        if (prize.getPointsCost() == null || prize.getPointsCost() <= 0) {
            throw new IllegalArgumentException("El premio no es canjeable");
        }

        if (user.getCurrentPoints() < prize.getPointsCost()) {
            throw new IllegalArgumentException("El usuario no tiene puntos suficientes");
        }

        int previousBalance = user.getCurrentPoints();
        int newBalance = previousBalance - prize.getPointsCost();

        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(TransactionType.SPEND);
        transaction.setPointsDelta(-prize.getPointsCost());
        transaction.setPreviousBalance(previousBalance);
        transaction.setNewBalance(newBalance);

        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);

        user.setCurrentPoints(newBalance);
        userRepository.save(user);

        PrizeRedemption prizeRedemption = new PrizeRedemption();
        prizeRedemption.setUser(user);
        prizeRedemption.setPrize(prize);
        prizeRedemption.setPointTransaction(savedTransaction);
        prizeRedemption.setPointsSpent(prize.getPointsCost());
        prizeRedemption.setCampaign(campaign);

        PrizeRedemption saved = repository.save(prizeRedemption);

        return mapper.toResponse(saved);
    }
}
