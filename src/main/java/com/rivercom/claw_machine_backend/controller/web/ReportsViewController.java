package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ReportsViewController {

    private final ReportsService reportsService;

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

    @GetMapping("/reports/weekly-summary")
    public String weeklySummaryPage(Model model) {
        ReportsService.WeeklyReportPeriod period = reportsService.getCurrentWeeklyReportPeriod();
        model.addAttribute("weekStart", period.weekStart());
        model.addAttribute("weekEnd", period.weekEnd());
        return "weekly-summary-report";
    }
}
