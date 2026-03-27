package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.dto.MachineCampaignResponseDTO;
import com.rivercom.claw_machine_backend.service.CampaignDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CampaignViewController {

    private final CampaignDetailService service;

    @GetMapping("/campaigns")
    public String campaigns(Model model) {
        List<MachineCampaignResponseDTO> campaigns = service.listAll();
        model.addAttribute("campaigns", campaigns);
        return "campaigns";
    }
}