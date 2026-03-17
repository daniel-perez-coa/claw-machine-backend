package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.dto.MachineCampaignResponse;
import com.rivercom.claw_machine_backend.mapper.MachineCampaignMapper;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineCampaignService {

    private final MachineCampaignRepository repository;
    private final MachineCampaignMapper mapper;

    public List<MachineCampaignResponse> listAll() {

        List<MachineCampaign> machineCampaigns = repository.findAll();

        return mapper.toResponseList(machineCampaigns);
    }
}
