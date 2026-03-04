package com.powermanage.repository;

import com.powermanage.entity.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    
    List<AlertRecord> findByUser_IdAndAlertTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    
    List<AlertRecord> findByUser_NameAndAlertTimeBetween(String username, LocalDateTime startTime, LocalDateTime endTime);

    
    List<AlertRecord> findByIsProcessedFalse();

    
    List<AlertRecord> findByIsProcessedTrue();

    
    List<AlertRecord> findByAlertType(String alertType);

    
    List<AlertRecord> findBySeverityLevel(String severityLevel);

    
    List<AlertRecord> findByAlertTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    
    List<AlertRecord> findByAlertTimeBetweenAndIsProcessedFalse(LocalDateTime startTime, LocalDateTime endTime);

    
    List<AlertRecord> findByDetectionRecordId(Long detectionRecordId);
}