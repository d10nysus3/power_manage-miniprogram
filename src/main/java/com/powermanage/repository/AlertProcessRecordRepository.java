package com.powermanage.repository;

import com.powermanage.entity.AlertProcessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertProcessRecordRepository extends JpaRepository<AlertProcessRecord, Long> {
    
    Optional<AlertProcessRecord> findByAlertRecordId(Long alertRecordId);

    
    List<AlertProcessRecord> findByProcessor(String processor);

    
    List<AlertProcessRecord> findByArchiveStatus(String archiveStatus);

    
    List<AlertProcessRecord> findByProcessingTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    
    List<AlertProcessRecord> findByHasSafetyHazardTrue();
    List<AlertProcessRecord> findByHasSafetyHazardFalse();
}