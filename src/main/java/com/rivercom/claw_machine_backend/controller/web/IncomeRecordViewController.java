package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.dto.IncomeRecordRequestDTO;
import com.rivercom.claw_machine_backend.service.IncomeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class IncomeRecordViewController {

    private final IncomeRecordService incomeRecordService;

    @GetMapping("/income-records/new")
    public String showIncomeRecordForm() {
        return "income-record-form";
    }

    @PostMapping("/income-records")
    public String createIncomeRecord(
            @ModelAttribute IncomeRecordRequestDTO request,
            RedirectAttributes redirectAttributes
    ) {
        incomeRecordService.incomeRecords(request);
        redirectAttributes.addFlashAttribute("successMessage", "Ingreso registrado correctamente.");
        return "redirect:/";
    }
}
