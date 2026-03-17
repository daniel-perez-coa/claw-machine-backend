package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.dto.MachineCampaignResponse;
import com.rivercom.claw_machine_backend.dto.MachineCampaignNewRequest;
import com.rivercom.claw_machine_backend.dto.MachineCampaignUpdateRequest;
import com.rivercom.claw_machine_backend.service.MachineCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/machine-campaigns")
public class MachineCampaignController {

    private final MachineCampaignService service;

    @GetMapping
    public ResponseEntity<List<MachineCampaignResponse>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping("/create-campaign")
    public ResponseEntity<MachineCampaignResponse> createCampaign (
            @Valid @RequestBody MachineCampaignNewRequest request) {
    return ResponseEntity.ok(service.createCampaign(request));
    }

    @PutMapping("/{id}/update-campaign")
    public ResponseEntity<MachineCampaignResponse> updateCampaign (
            @Valid @PathVariable Long id,
            @Valid @RequestBody MachineCampaignUpdateRequest request) {
        return ResponseEntity.ok(service.updateCampaign(request,id));
    }
}
