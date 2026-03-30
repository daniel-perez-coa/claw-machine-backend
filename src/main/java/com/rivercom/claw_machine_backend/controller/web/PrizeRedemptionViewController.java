package com.rivercom.claw_machine_backend.controller.web;

import com.rivercom.claw_machine_backend.dto.PrizeDTO;
import com.rivercom.claw_machine_backend.dto.PrizeRedemptionResponseDTO;
import com.rivercom.claw_machine_backend.dto.UserRedemptionRequestDTO;
import com.rivercom.claw_machine_backend.service.PrizeRedemptionService;
import com.rivercom.claw_machine_backend.service.PrizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/redemptions")
public class PrizeRedemptionViewController {

    private final PrizeService prizeService;
    private final PrizeRedemptionService prizeRedemptionService;

    @GetMapping
    public String redemptionsPage(Model model) {
        loadFormData(model);

        if (!model.containsAttribute("redemptionForm")) {
            model.addAttribute("redemptionForm", new UserRedemptionRequestDTO("", null));
        }

        return "redemptions";
    }

    @PostMapping
    public String redeemPrize(
            @Valid @ModelAttribute("redemptionForm") UserRedemptionRequestDTO request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        List<PrizeDTO> prizes = prizeService.getAllPrizes();
        model.addAttribute("prizes", prizes);

        if (bindingResult.hasErrors()) {
            return "redemptions";
        }

        try {
            PrizeRedemptionResponseDTO response = prizeRedemptionService.spentPoints(request);
            redirectAttributes.addFlashAttribute("successMessage", "Canje realizado correctamente.");
            redirectAttributes.addFlashAttribute("redemptionResult", response);
            return "redirect:/redemptions";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "redemptions";
        }
    }

    private void loadFormData(Model model) {
        List<PrizeDTO> prizes = prizeService.getAllPrizes();
        model.addAttribute("prizes", prizes);
    }
}