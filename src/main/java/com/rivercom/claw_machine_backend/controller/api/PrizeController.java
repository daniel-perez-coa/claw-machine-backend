package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.NewPrizeDTO;
import com.rivercom.claw_machine_backend.dto.PrizeLookupDTO;
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

    @GetMapping("/inactive")
    public ResponseEntity<List<PrizeDTO>> getInactivePrizes() {
        return ResponseEntity.ok(service.getInactivePrizes());
    }

    @GetMapping("/by-name")
    public ResponseEntity<PrizeLookupDTO> getPrizeByName(@RequestParam String name) {
        return ResponseEntity.ok(service.getPrizeByName(name));
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

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<PrizeDTO> reactivatePrize(
            @PathVariable Long id,
            @RequestBody(required = false) NewPrizeDTO prizeDTO
    ) {
        return ResponseEntity.ok(service.reactivatePrize(id, prizeDTO));
    }
}
