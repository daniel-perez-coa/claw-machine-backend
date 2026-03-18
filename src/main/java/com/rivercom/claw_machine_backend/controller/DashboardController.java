package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.dto.DashboardInformationDTO;
import com.rivercom.claw_machine_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardInformationDTO> getDashboardInformation() {
        DashboardInformationDTO response = dashboardService.getDashboardInformation();
        return ResponseEntity.ok(response);
    }
}
