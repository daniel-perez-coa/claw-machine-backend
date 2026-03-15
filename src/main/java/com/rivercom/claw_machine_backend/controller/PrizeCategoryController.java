package com.rivercom.claw_machine_backend.controller;

import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rivercom.claw_machine_backend.service.PrizeCategoryService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/prize-categories")
public class PrizeCategoryController {

    private final PrizeCategoryService service;

    @GetMapping
    public List<PrizeCategory> findAll() {
        return service.findAll();
    }

}
