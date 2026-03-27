package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.dto.DashboardInformationDTO;
import com.rivercom.claw_machine_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardViewController {

    private final DashboardService service;

    @GetMapping("/")
    public String dashboard(Model model) {
        DashboardInformationDTO dashboard =
                service.getDashboardInformation();

        model.addAttribute("dashboard", dashboard);
        return "dashboard";
    }
}
