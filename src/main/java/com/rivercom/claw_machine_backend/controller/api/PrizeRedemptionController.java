package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.PrizeRedemptionResponseDTO;
import com.rivercom.claw_machine_backend.dto.UserRedemptionRequestDTO;
import com.rivercom.claw_machine_backend.service.PrizeRedemptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/prize-redemption")
public class PrizeRedemptionController {

    private final PrizeRedemptionService service;

    @PostMapping
    ResponseEntity<PrizeRedemptionResponseDTO> prizeRedemption(
            @RequestBody UserRedemptionRequestDTO request) {
        return ResponseEntity.ok(service.spentPoints(request));
    }
}
