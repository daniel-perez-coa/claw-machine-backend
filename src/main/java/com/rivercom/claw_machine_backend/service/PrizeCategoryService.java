package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.rivercom.claw_machine_backend.repository.PrizeCategoryRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PrizeCategoryService {

    private final PrizeCategoryRepository repository;

    public List<PrizeCategory> findAll() {
        return repository.findAll();
    }
}
