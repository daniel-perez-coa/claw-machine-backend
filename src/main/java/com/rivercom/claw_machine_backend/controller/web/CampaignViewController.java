package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CampaignViewController {

    @GetMapping("/campaigns")
    public String campaignsPage() {
        return "campaigns";
    }
}
