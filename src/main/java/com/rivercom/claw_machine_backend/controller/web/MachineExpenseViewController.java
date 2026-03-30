package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.dto.MachineExpenseFormDTO;
import com.rivercom.claw_machine_backend.service.MachineExpenseRecordsService;
import com.rivercom.claw_machine_backend.service.PrizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MachineExpenseViewController {

    private final MachineExpenseRecordsService expenseService;
    private final PrizeService prizeService;


    @GetMapping("/machine-expense-records/new")
    public String showExpenseForm(Model model) {
        model.addAttribute("prizes", prizeService.getActivePrizes());
        return "machine-expense-form";
    }

    @PostMapping("/machine-expense-records")
    public String registerExpenses(
            @ModelAttribute("request") MachineExpenseFormDTO request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            expenseService.registerExpenses(request.items());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Canje registrado correctamente."
            );

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
            return "redirect:/machine-expense-records/new";
        }

        return "redirect:/";
    }
}
