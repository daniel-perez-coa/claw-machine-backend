package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}
