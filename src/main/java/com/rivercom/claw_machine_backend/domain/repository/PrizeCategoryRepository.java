package com.rivercom.claw_machine_backend.domain.repository;

import com.rivercom.claw_machine_backend.domain.entity.PrizeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrizeCategoryRepository extends JpaRepository<PrizeCategory, Long> {

    Optional<PrizeCategory> findByCode(String code);

}
