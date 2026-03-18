package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineExpenseRecordsRepository extends JpaRepository<MachineExpenseRecords, Long> {
    List<MachineExpenseRecords> findByCampaignId(Long campaignId);

    List<MachineExpenseRecords> findByRestocked(Boolean restocked);
}
