package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.enums.MachineCampaignStatus;
import com.rivercom.claw_machine_backend.dto.MachineCampaignResponseDTO;
import com.rivercom.claw_machine_backend.dto.MachineCampaignNewCampaignDTO;
import com.rivercom.claw_machine_backend.dto.MachineCampaignUpdateRequestDTO;
import com.rivercom.claw_machine_backend.mapper.MachineCampaignMapper;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignDetailService {

    private final MachineCampaignRepository repository;
    private final PrizeRepository prizeRepository;
    private final MachineCampaignMapper mapper;

    public List<MachineCampaignResponseDTO> listAll() {

        List<MachineCampaign> machineCampaigns = repository.findAll();

        return mapper.toResponseList(machineCampaigns);
    }

    public MachineCampaignResponseDTO createCampaign
            (MachineCampaignNewCampaignDTO request) {
        Optional<Prize> prize = prizeRepository.findById(request.majorPrizeId());
        Optional<MachineCampaign> existingOpenCampaign =
                repository.findByStatus(MachineCampaignStatus.OPEN);

        if (existingOpenCampaign.isPresent()) {
            MachineCampaign openCampaign = existingOpenCampaign.get();
            log.warn("No se puede crear la campaña {} porque ya existe una campaña OPEN: {}",
                    request.name(), openCampaign.getName());
            return  null;
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
        return mapper.toResponse(savedCampaign);
    }

    public MachineCampaignResponseDTO updateCampaign (MachineCampaignUpdateRequestDTO request, Long id) {
        Optional<MachineCampaign> existingCampaign =
                repository.findById(id);
        if (existingCampaign.isEmpty()) {
            log.warn("No se ha encontrado ninguna campaña con el ID {}",id);
            return null;
        }

        MachineCampaign campaign = existingCampaign.get();

        if (request.status() == MachineCampaignStatus.CLOSED) {
            campaign.setStatus(MachineCampaignStatus.CLOSED);
            campaign.setClosedAt(LocalDateTime.from(LocalDate.now()));
            campaign.setNotes("Campaña cerrada exitosamente porque se logró la meta.");
        }
        if (request.status() == MachineCampaignStatus.CANCELLED) {
            campaign.setStatus(MachineCampaignStatus.CANCELLED);
            campaign.setNotes(request.notes());
            campaign.setClosedAt(LocalDateTime.from(LocalDate.now()));
        }
        MachineCampaign updatedCampaign = repository.save(campaign);
        return mapper.toResponse(updatedCampaign);
    }
}
