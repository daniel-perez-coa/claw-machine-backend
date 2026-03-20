package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeCategoryRepository extends JpaRepository<PrizeCategory, Long> {
    PrizeCategory findByName(String name);
}
