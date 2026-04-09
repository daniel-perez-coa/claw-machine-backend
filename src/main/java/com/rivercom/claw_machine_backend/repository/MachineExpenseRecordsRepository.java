package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MachineExpenseRecordsRepository extends JpaRepository<MachineExpenseRecords, Long> {

    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    List<MachineExpenseRecords> findByCampaignId(Long campaignId);

    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    List<MachineExpenseRecords> findByCampaignIdOrderByRegisteredAtDesc(Long campaignId);

    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    List<MachineExpenseRecords> findByRestocked(Boolean restocked);

    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    List<MachineExpenseRecords> findByCampaignIdAndRestocked(Long campaignId, Boolean restocked);

    @Override
    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    List<MachineExpenseRecords> findAll();

    @Override
    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    Optional<MachineExpenseRecords> findById(Long id);

    @EntityGraph(attributePaths = {"prize", "prize.category", "campaign", "campaign.majorPrize"})
    List<MachineExpenseRecords> findByIdIn(List<Long> ids);
}
