package com.powermanage.service;

import com.powermanage.entity.AlertRecord;
import com.powermanage.entity.AlertProcessRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AlertService {
    
    List<AlertRecord> getAllAlertRecords();

    
    List<AlertRecord> getUnprocessedAlertRecords();

    
    List<AlertRecord> getAlertRecordsByUserId(Long userId);

    
    List<AlertRecord> getAlertRecordsByUsername(String username);
    
    List<AlertRecord> getAlertRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    
    List<AlertRecord> getAlertRecordsBySeverityLevel(String severityLevel);
    
    
    AlertRecord getAlertRecordById(Long alertId);

    
    AlertProcessRecord processAlert(Long alertId, AlertProcessRecord processRecord);

    
    AlertProcessRecord getAlertProcessRecordByAlertId(Long alertId);

    
    List<AlertProcessRecord> getAlertProcessRecordsByArchiveStatus(String archiveStatus);

    
    Map<String, Object> generateMonthlyAlertReport(int year, int month);

    
    List<AlertRecord> getRealtimeAlerts();

    
    Map<String, Object> getAlertStats();
    
    
    AlertProcessRecord closeAlert(Long alertId, AlertProcessRecord closeRecord);
    
    
    AlertRecord saveAlertRecord(AlertRecord alertRecord);
    
    
    AlertRecord markAsFalseAlarm(Long alertId);
}