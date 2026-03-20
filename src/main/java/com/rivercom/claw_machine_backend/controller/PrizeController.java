package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.dto.NewPrizeDTO;
import com.rivercom.claw_machine_backend.dto.PrizeDTO;
import com.rivercom.claw_machine_backend.service.PrizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/prizes")
public class PrizeController {

    private final PrizeService service;

    @GetMapping
    public ResponseEntity<List<PrizeDTO>> getAllPrizes() {
        return ResponseEntity.ok(service.getAllPrizes());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PrizeDTO>> getActivePrizes() {
        return ResponseEntity.ok(service.getActivePrizes());
    }

    @PostMapping
    public ResponseEntity<PrizeDTO> createPrize(@RequestBody NewPrizeDTO newPrize) {
        return ResponseEntity.ok(service.createPrize(newPrize));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrizeDTO> updatePrize(
            @PathVariable Long id,
            @RequestBody PrizeDTO prizeDTO
    ) {
        return ResponseEntity.ok(service.updatePrize(prizeDTO, id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PrizeDTO> deactivatePrize(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivatePrize(id));
    }
}
