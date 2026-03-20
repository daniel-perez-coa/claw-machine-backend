package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.Prize;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrizeRepository extends JpaRepository<Prize, Long> {

    @EntityGraph(attributePaths = "category")
    List<Prize> findByIsActive(Boolean isActive);

    Optional<Prize> findByName(String name);

    @EntityGraph(attributePaths = "category")
    List<Prize> findByCategoryCode(String code);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Prize> findAll();
}
