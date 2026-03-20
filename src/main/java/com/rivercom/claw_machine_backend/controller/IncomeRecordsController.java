package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.dto.IncomeRecordRequestDTO;
import com.rivercom.claw_machine_backend.service.IncomeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/income-records")
public class IncomeRecordsController {

    private final IncomeRecordService service;

    @PostMapping
    public ResponseEntity<?> incomeRecords(
            @RequestBody IncomeRecordRequestDTO recordRequestDTO) {
        service.incomeRecords(recordRequestDTO);
        return ResponseEntity.ok().build();
    }
}
