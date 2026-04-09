package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportsViewController {

    @GetMapping("/reports")
    public String reportsPage() {
        return "reports";
    }

    @GetMapping("/reports/campaign-add-points")
    public String campaignAddPointsPage() {
        return "campaign-add-points-report";
    }

    @GetMapping("/reports/campaign-redemptions")
    public String campaignRedemptionsPage() {
        return "campaign-redemptions-report";
    }

    @GetMapping("/reports/campaign-quick-redemptions")
    public String campaignQuickRedemptionsPage() {
        return "campaign-quick-redemptions-report";
    }
}
