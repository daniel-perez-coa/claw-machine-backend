package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.MachineExpenseDTO;
import com.rivercom.claw_machine_backend.dto.MachineExpenseFormDTO;
import com.rivercom.claw_machine_backend.dto.MachineExpenseRegistrationResponseDTO;
import com.rivercom.claw_machine_backend.dto.MachineExpenseRequestDTO;
import com.rivercom.claw_machine_backend.service.MachineExpenseRecordsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/machine-expense-records")
@RequiredArgsConstructor
public class MachineExpenseRecordsController {

    private final MachineExpenseRecordsService service;

    @PostMapping
    public ResponseEntity<MachineExpenseRegistrationResponseDTO> registerExpense(
            @Valid @RequestBody MachineExpenseFormDTO request) {
        MachineExpenseRegistrationResponseDTO response = service.registerExpenses(request.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<MachineExpenseDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/open-campaign")
    public List<MachineExpenseDTO> findOpenCampaignExpenses() {
        return service.findOpenCampaignExpenses();
    }

    @GetMapping("/pending-restock")
    public List<MachineExpenseDTO> findPendingRestock() {
        return service.findPendingRestock();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MachineExpenseDTO> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody MachineExpenseRequestDTO request) {
        return ResponseEntity.ok(service.updateExpense(id, request));
    }

    @PatchMapping("/{id}/restocked")
    public ResponseEntity<Map<String, String>> markAsRestocked(@PathVariable Long id) {
        service.markAsRestocked(id);
        return ResponseEntity.ok(Map.of("message", "Marcado como resurtido con exito"));
    }
}
