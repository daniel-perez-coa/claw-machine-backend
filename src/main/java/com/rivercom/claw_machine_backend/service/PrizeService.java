package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;
import com.rivercom.claw_machine_backend.dto.NewPrizeDTO;
import com.rivercom.claw_machine_backend.dto.PrizeLookupDTO;
import com.rivercom.claw_machine_backend.dto.PrizeDTO;
import com.rivercom.claw_machine_backend.mapper.PrizeMapper;
import com.rivercom.claw_machine_backend.repository.PrizeCategoryRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrizeService {

    private final PrizeRepository repository;
    private final PrizeMapper mapper;
    private final PrizeCategoryRepository prizeCategoryRepository;

    public List<PrizeDTO> getActivePrizes() {
        return mapper.toResponsePrizeList(repository.findByIsActive(true));
    }

    public List<PrizeDTO> getAllPrizes() {
        return mapper.toResponsePrizeList(repository.findAll());
    }

    public List<PrizeDTO> getInactivePrizes() {
        return mapper.toResponsePrizeList(repository.findByIsActive(false));
    }

    public PrizeLookupDTO getPrizeByName(String name) {
        String normalizedName = normalizePrizeName(name);

        Prize prize = repository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El premio no existe"));

        return new PrizeLookupDTO(prize.getId(), prize.getName(), prize.getIsActive());
    }

    @Transactional
    public PrizeDTO createPrize(NewPrizeDTO prize) {
        String normalizedName = normalizePrizeName(prize.name());
        Optional<Prize> existingPrize = repository.findByNameIgnoreCase(normalizedName);
        if (existingPrize.isPresent()) {
            log.error("Prize already exists");
            throw new ResponseStatusException(CONFLICT, "Ya existe un premio con ese nombre");
        }

        PrizeCategory prizeCategory = Optional.ofNullable(
                prizeCategoryRepository.findByCode(prize.prizeCategory())
        ).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "La categoria no existe"));

        Prize newPrize = new Prize();
        newPrize.setCategory(prizeCategory);
        newPrize.setName(normalizedName);
        newPrize.setDescription(normalizeText(prize.description()));
        newPrize.setPointsCost(prize.pointsCost());
        newPrize.setIsActive(true);
        newPrize.setCost(prize.cost());

        Prize savedPrize = repository.save(newPrize);
        return mapper.toResponsePrize(savedPrize);
    }

    @Transactional
    public PrizeDTO updatePrize(PrizeDTO prize, Long id) {
        Optional<Prize> existingPrize = repository.findById(id);
        if (existingPrize.isEmpty()) {
            log.error("Prize not found");
            throw new ResponseStatusException(BAD_REQUEST, "El premio no existe");
        }

        String normalizedName = normalizePrizeName(prize.name());

        String prizeCategoryCode = Optional.ofNullable(prize.prizeCode())
                .filter(code -> !code.isBlank())
                .orElse(prize.prizeCategory());

        PrizeCategory prizeCategory = Optional.ofNullable(
                prizeCategoryRepository.findByCode(prizeCategoryCode)
        ).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "La categoria no existe"));

        if (repository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            log.error("Prize already exists");
            throw new ResponseStatusException(CONFLICT, "Ya existe un premio con ese nombre");
        }

        Prize updatedPrize = existingPrize.get();
        updatedPrize.setName(normalizedName);
        updatedPrize.setDescription(normalizeText(prize.description()));
        updatedPrize.setPointsCost(prize.pointsCost());
        updatedPrize.setCost(prize.cost());
        updatedPrize.setCategory(prizeCategory);

        Prize savedPrize = repository.save(updatedPrize);
        return mapper.toResponsePrize(savedPrize);
    }

    @Transactional
    public PrizeDTO deactivatePrize(Long id) {
        Prize prize = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("El premio no existe"));

        if (!prize.getIsActive()) {
            throw new IllegalStateException("El premio ya esta desactivado");
        }

        prize.setIsActive(false);

        Prize updatedPrize = repository.save(prize);
        return mapper.toResponsePrize(updatedPrize);
    }

    @Transactional
    public PrizeDTO reactivatePrize(Long id, NewPrizeDTO prizeRequest) {
        Prize prize = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El premio no existe"));

        if (Boolean.TRUE.equals(prize.getIsActive())) {
            throw new ResponseStatusException(CONFLICT, "El premio ya esta activo");
        }

        if (prizeRequest != null) {
            String normalizedName = normalizePrizeName(prizeRequest.name());

            if (!prize.getName().equalsIgnoreCase(normalizedName)) {
                throw new ResponseStatusException(BAD_REQUEST, "El premio a reactivar no coincide con el nombre solicitado");
            }

            PrizeCategory prizeCategory = Optional.ofNullable(
                    prizeCategoryRepository.findByCode(prizeRequest.prizeCategory())
            ).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "La categoria no existe"));

            prize.setCategory(prizeCategory);
            prize.setName(normalizedName);
            prize.setDescription(normalizeText(prizeRequest.description()));
            prize.setPointsCost(prizeRequest.pointsCost());
            prize.setCost(prizeRequest.cost());
        }

        prize.setIsActive(true);

        Prize updatedPrize = repository.save(prize);
        return mapper.toResponsePrize(updatedPrize);
    }

    private String normalizePrizeName(String name) {
        return Optional.ofNullable(name)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El nombre del premio es obligatorio"));
    }

    private String normalizeText(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .orElse(null);
    }
}
