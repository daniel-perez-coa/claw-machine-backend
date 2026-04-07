package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MachineExpenseViewController {

    @GetMapping("/machine-expense-records/new")
    public String showExpenseForm() {
        return "machine-expense-form";
    }
}
