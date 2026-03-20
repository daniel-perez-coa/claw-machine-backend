package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;
import com.rivercom.claw_machine_backend.dto.NewPrizeDTO;
import com.rivercom.claw_machine_backend.dto.PrizeDTO;
import com.rivercom.claw_machine_backend.mapper.PrizeMapper;
import com.rivercom.claw_machine_backend.repository.PrizeCategoryRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrizeService {

    private final PrizeRepository repository;
    private final PrizeMapper mapper;
    private final PrizeCategoryRepository prizeCategoryRepository;

    public List<PrizeDTO> getActivePrizes() {
         return mapper.toResponsePrizeList(
                 repository.findByIsActive(true));
    }

    public List<PrizeDTO> getAllPrizes() {
        return mapper.toResponsePrizeList(
                repository.findAll());
    }

    public PrizeDTO createPrize(NewPrizeDTO prize) {
        Optional<Prize> existingPrize = repository.findByName(prize.name());
        if (existingPrize.isPresent()) {
            log.error("Prize already exists");
            return null;
        }
        Prize newPrize = new Prize();
        PrizeCategory prizeCategory = Optional.ofNullable(
                prizeCategoryRepository.findByName(prize.prizeCategory())
        ).orElseThrow(() -> new IllegalArgumentException("La categoría no existe"));
        newPrize.setCategory(prizeCategory);
        newPrize.setName(prize.name());
        newPrize.setDescription(prize.description());
        newPrize.setPointsCost(prize.pointsCost());
        newPrize.setIsActive(true);
        newPrize.setCost(prize.cost());

        Prize savedPrize = repository.save(newPrize);
        return mapper.toResponsePrize(savedPrize);
    }

    public PrizeDTO updatePrize(PrizeDTO prize, Long id) {
        Optional<Prize> existingPrize = repository.findById(id);
        if (existingPrize.isEmpty()) {
            log.error("Prize not found");
            return null;
        }
        Prize updatedPrize = existingPrize.get();
        updatedPrize.setName(prize.name());
        updatedPrize.setDescription(prize.description());
        updatedPrize.setPointsCost(prize.pointsCost());
        updatedPrize.setCost(prize.cost());
        updatedPrize.setCategory(prize.prizeCategory());
        Prize savedPrize = repository.save(updatedPrize);
        return mapper.toResponsePrize(savedPrize);
    }

    public PrizeDTO deactivatePrize(Long id) {
        Prize prize = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("El premio no existe"));

        if (!prize.getIsActive()) {
            throw new IllegalStateException("El premio ya está desactivado");
        }

        prize.setIsActive(false);

        Prize updatedPrize = repository.save(prize);
        return mapper.toResponsePrize(updatedPrize);
    }
}
