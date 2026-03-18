package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.dto.MachineCampaignResponseDTO;
import com.rivercom.claw_machine_backend.dto.MachineCampaignNewCampaignDTO;
import com.rivercom.claw_machine_backend.dto.MachineCampaignUpdateRequestDTO;
import com.rivercom.claw_machine_backend.service.CampaignDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/campaign-detail")
public class CampaignDetailController {

    private final CampaignDetailService service;

    @GetMapping
    public ResponseEntity<List<MachineCampaignResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping("/machine-campaign/create-campaign")
    public ResponseEntity<MachineCampaignResponseDTO> createCampaign (
            @Valid @RequestBody MachineCampaignNewCampaignDTO request) {
    return ResponseEntity.ok(service.createCampaign(request));
    }

    @PutMapping("/machine-campaign/{id}/update-campaign")
    public ResponseEntity<MachineCampaignResponseDTO> updateCampaign (
            @Valid @PathVariable Long id,
            @Valid @RequestBody MachineCampaignUpdateRequestDTO request) {
        return ResponseEntity.ok(service.updateCampaign(request,id));
    }
}
