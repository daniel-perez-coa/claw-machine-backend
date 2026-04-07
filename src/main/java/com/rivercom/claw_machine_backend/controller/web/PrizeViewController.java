package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/prizes")
public class PrizeViewController {

    @GetMapping
    public String prizesPage() {
        return "prizes";
    }

    @GetMapping("/new")
    public String newPrizePage() {
        return "prize-form";
    }

    @GetMapping("/{id}")
    public String updatePrizePage(@PathVariable Long id) {
        return "prize-update-form";
    }
}
