package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.MachineCampaignResponseDTO;
import com.rivercom.claw_machine_backend.dto.NewMachineCampaignCampaignDTO;
import com.rivercom.claw_machine_backend.dto.MachineCampaignUpdateRequestDTO;
import com.rivercom.claw_machine_backend.service.CampaignDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/campaigns")
public class MachineCampaignController {

    private final CampaignDetailService service;

    @GetMapping
    public ResponseEntity<List<MachineCampaignResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping
    public ResponseEntity<MachineCampaignResponseDTO> createCampaign(
            @Valid @RequestBody NewMachineCampaignCampaignDTO request) {
        return ResponseEntity.ok(service.createCampaign(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MachineCampaignResponseDTO> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody MachineCampaignUpdateRequestDTO request) {
        return ResponseEntity.ok(service.updateCampaign(request, id));
    }
}
