package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.dto.MachineCampaignResponse;
import com.rivercom.claw_machine_backend.service.MachineCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/machine-campaigns")
public class MachineCampaignController {

    private final MachineCampaignService service;

    @GetMapping
    public ResponseEntity<List<MachineCampaignResponse>> findAll() {
        return ResponseEntity.ok(service.listAll());
    }
}
