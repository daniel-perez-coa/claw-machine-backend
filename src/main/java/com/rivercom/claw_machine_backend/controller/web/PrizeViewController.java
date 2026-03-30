package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.service.PrizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PrizeViewController {

    private final PrizeService service;

    @GetMapping("/prizes")
    public String showPrizesPage(Model model) {
        model.addAttribute("prizes", service.getAllPrizes());
        return "prizes";
    }
}