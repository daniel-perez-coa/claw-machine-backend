package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/campaigns")
public class CampaignViewController {

    @GetMapping
    public String campaignsPage() {
        return "campaigns";
    }

    @GetMapping("/new")
    public String newCampaignPage() {
        return "campaign-form";
    }

    @GetMapping("/{id}")
    public String editCampaignPage(@PathVariable Long id) {
        return "campaign-update-form";
    }
}
