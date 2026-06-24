package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.IncomeRecords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IncomeRecordsRepository extends JpaRepository<IncomeRecords, Long> {

    List<IncomeRecords> findByCampaignId(Long campaignId);

    List<IncomeRecords> findByRegisteredAtBetween(LocalDateTime start, LocalDateTime end);
}
