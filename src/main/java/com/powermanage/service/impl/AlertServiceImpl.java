package com.powermanage.service.impl;

import com.powermanage.entity.AlertRecord;
import com.powermanage.entity.AlertProcessRecord;
import com.powermanage.repository.AlertRecordRepository;
import com.powermanage.repository.AlertProcessRecordRepository;
import com.powermanage.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRecordRepository alertRecordRepository;

    @Autowired
    private AlertProcessRecordRepository alertProcessRecordRepository;

    @Override
    public List<AlertRecord> getAllAlertRecords() {
        return alertRecordRepository.findAll();
    }

    @Override
    public List<AlertRecord> getUnprocessedAlertRecords() {
        return alertRecordRepository.findByIsProcessedFalse();
    }

    @Override
    public List<AlertRecord> getAlertRecordsByUserId(Long userId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMonths(3); 
        return alertRecordRepository.findByUser_IdAndAlertTimeBetween(userId, startTime, endTime);
    }

    @Override
    public List<AlertRecord> getAlertRecordsByUsername(String username) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMonths(3); 
        return alertRecordRepository.findByUser_NameAndAlertTimeBetween(username, startTime, endTime);
    }

    @Override
    public List<AlertRecord> getAlertRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return alertRecordRepository.findByAlertTimeBetween(startTime, endTime);
    }

    @Override
    public List<AlertRecord> getAlertRecordsBySeverityLevel(String severityLevel) {
        return alertRecordRepository.findBySeverityLevel(severityLevel);
    }

    @Override
    public AlertRecord getAlertRecordById(Long alertId) {
        return alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
    }

    @Override
    public AlertProcessRecord processAlert(Long alertId, AlertProcessRecord processRecord) {
        
        AlertRecord alertRecord = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        
        if (alertRecord.getIsProcessed()) {
            throw new RuntimeException("Alert has already been processed");
        }

        
        Optional<AlertProcessRecord> existingRecord = alertProcessRecordRepository.findByAlertRecordId(alertId);
        if (existingRecord.isPresent()) {
            throw new RuntimeException("Alert process record already exists for alert id: " + alertId);
        }

        
        processRecord.setAlertRecord(alertRecord);
        processRecord.setAlertType(alertRecord.getAlertType());
        processRecord.setProcessingTime(LocalDateTime.now());
        processRecord.setArchiveStatus("active");

        
        alertRecord.setIsProcessed(true);
        alertRecord.setProcessTime(LocalDateTime.now());
        alertRecord.setProcessor(processRecord.getProcessor());
        

        
        alertRecordRepository.save(alertRecord);
        return alertProcessRecordRepository.save(processRecord);
    }

    @Override
    public AlertProcessRecord getAlertProcessRecordByAlertId(Long alertId) {
        return alertProcessRecordRepository.findByAlertRecordId(alertId).orElse(null);
    }

    @Override
    public List<AlertProcessRecord> getAlertProcessRecordsByArchiveStatus(String archiveStatus) {
        return alertProcessRecordRepository.findByArchiveStatus(archiveStatus);
    }

    @Override
    public Map<String, Object> generateMonthlyAlertReport(int year, int month) {
        Map<String, Object> report = new HashMap<>();

        
        LocalDateTime startOfMonth = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).minusNanos(1);

        
        List<AlertRecord> monthlyAlerts = alertRecordRepository.findByAlertTimeBetween(startOfMonth, endOfMonth);
        int totalAlerts = monthlyAlerts.size();

        
        List<AlertRecord> processedAlerts = monthlyAlerts.stream()
                .filter(AlertRecord::getIsProcessed)
                .collect(Collectors.toList());
        int processedCount = processedAlerts.size();

        
        double processingRate = totalAlerts > 0 ? (double) processedCount / totalAlerts * 100 : 0;

        
        Map<String, Long> alertTypeStats = monthlyAlerts.stream()
                .collect(Collectors.groupingBy(AlertRecord::getAlertType, Collectors.counting()));

        
        Map<String, Long> severityStats = monthlyAlerts.stream()
                .collect(Collectors.groupingBy(AlertRecord::getSeverityLevel, Collectors.counting()));

        
        
        List<AlertProcessRecord> monthlyProcessRecords = alertProcessRecordRepository.findByProcessingTimeBetween(startOfMonth, endOfMonth);
        int totalProcessedRecords = monthlyProcessRecords.size();
        long falseAlarmCount = monthlyProcessRecords.stream()
                .filter(record -> !record.getHasSafetyHazard())
                .count();
        double falseAlarmRate = totalProcessedRecords > 0 ? (double) falseAlarmCount / totalProcessedRecords * 100 : 0.0;

        
        double avgProcessingTime = 0.0;
        if (!processedAlerts.isEmpty()) {
            long totalProcessingTimeMinutes = processedAlerts.stream()
                    .mapToLong(a -> java.time.Duration.between(a.getAlertTime(), a.getProcessTime()).toMinutes())
                    .sum();
            avgProcessingTime = (double) totalProcessingTimeMinutes / processedAlerts.size();
        }

        
        report.put("year", year);
        report.put("month", month);
        report.put("totalAlerts", totalAlerts);
        report.put("processedCount", processedCount);
        report.put("processingRate", String.format("%.2f%%", processingRate));
        report.put("falseAlarmRate", String.format("%.2f%%", falseAlarmRate));
        report.put("avgProcessingTimeMinutes", String.format("%.2f", avgProcessingTime));
        report.put("alertTypeStats", alertTypeStats);
        report.put("severityStats", severityStats);

        return report;
    }

    @Override
    public List<AlertRecord> getRealtimeAlerts() {
        
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return alertRecordRepository.findByAlertTimeBetweenAndIsProcessedFalse(oneHourAgo, LocalDateTime.now());
    }

    @Override
    public Map<String, Object> getAlertStats() {
        List<AlertRecord> allAlerts = alertRecordRepository.findAll();
        
        
        int total = allAlerts.size();
        int unprocessed = (int) allAlerts.stream()
                .filter(alert -> !alert.getIsProcessed())
                .count();
        
        
        int highRisk = (int) allAlerts.stream()
                .filter(alert -> "高".equals(alert.getSeverityLevel()) || "HIGH".equals(alert.getSeverityLevel()))
                .count();
        
        int mediumRisk = (int) allAlerts.stream()
                .filter(alert -> "中".equals(alert.getSeverityLevel()) || "MEDIUM".equals(alert.getSeverityLevel()))
                .count();
        
        int lowRisk = (int) allAlerts.stream()
                .filter(alert -> "低".equals(alert.getSeverityLevel()) || "LOW".equals(alert.getSeverityLevel()))
                .count();
        
        
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int processedToday = (int) allAlerts.stream()
                .filter(alert -> alert.getIsProcessed() && 
                        alert.getProcessTime() != null && 
                        alert.getProcessTime().isAfter(todayStart))
                .count();
        
        
        double avgResponseTime = 0.0;
        List<AlertRecord> processedAlerts = allAlerts.stream()
                .filter(alert -> alert.getIsProcessed() && alert.getProcessTime() != null)
                .collect(Collectors.toList());
        
        if (!processedAlerts.isEmpty()) {
            long totalResponseTimeMinutes = processedAlerts.stream()
                    .mapToLong(alert -> java.time.Duration.between(alert.getAlertTime(), alert.getProcessTime()).toMinutes())
                    .sum();
            avgResponseTime = (double) totalResponseTimeMinutes / processedAlerts.size();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("unprocessed", unprocessed);
        stats.put("highRisk", highRisk);
        stats.put("mediumRisk", mediumRisk);
        stats.put("lowRisk", lowRisk);
        stats.put("processedToday", processedToday);
        stats.put("avgResponseTimeMinutes", String.format("%.2f", avgResponseTime));
        
        return stats;
    }
    
    @Override
    public AlertProcessRecord closeAlert(Long alertId, AlertProcessRecord closeRecord) {
        
        AlertRecord alertRecord = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        
        Optional<AlertProcessRecord> existingRecord = alertProcessRecordRepository.findByAlertRecordId(alertId);
        if (existingRecord.isPresent()) {
            
            AlertProcessRecord recordToUpdate = existingRecord.get();
            recordToUpdate.setArchiveStatus("closed");
            if (closeRecord.getProcessor() != null) {
                recordToUpdate.setProcessor(closeRecord.getProcessor());
            }
            if (closeRecord.getProcessingResult() != null) {
                recordToUpdate.setProcessingResult(closeRecord.getProcessingResult());
            }
            recordToUpdate.setProcessingTime(LocalDateTime.now());
            
            
            alertRecord.setIsProcessed(true);
            alertRecord.setProcessTime(LocalDateTime.now());
            if (closeRecord.getProcessor() != null) {
                alertRecord.setProcessor(closeRecord.getProcessor());
            }
            
            
            alertRecordRepository.save(alertRecord);
            return alertProcessRecordRepository.save(recordToUpdate);
        }
        
        
        alertRecord.setIsProcessed(true);
        alertRecord.setProcessTime(LocalDateTime.now());
        
        if (closeRecord.getProcessor() != null) {
            alertRecord.setProcessor(closeRecord.getProcessor());
        }
        
        
        closeRecord.setAlertRecord(alertRecord);
        closeRecord.setAlertType(alertRecord.getAlertType());
        closeRecord.setProcessingTime(LocalDateTime.now());
        closeRecord.setArchiveStatus("closed");
        
        
        alertRecordRepository.save(alertRecord);
        return alertProcessRecordRepository.save(closeRecord);
    }
    
    @Override
    public AlertRecord saveAlertRecord(AlertRecord alertRecord) {
        System.out.println("AlertServiceImpl.saveAlertRecord 被调用，记录ID: " + alertRecord.getId() + ", 当前已读状态: " + alertRecord.getIsRead());
        AlertRecord savedRecord = alertRecordRepository.save(alertRecord);
        System.out.println("告警记录保存成功，保存后的已读状态: " + savedRecord.getIsRead());
        return savedRecord;
    }
    
    @Override
    public AlertRecord markAsFalseAlarm(Long alertId) {
        AlertRecord alertRecord = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        
        alertRecord.setIsFalseAlarm(true);
        alertRecord.setIsProcessed(true);
        alertRecord.setProcessTime(LocalDateTime.now());
        
        
        Optional<AlertProcessRecord> existingRecord = alertProcessRecordRepository.findByAlertRecordId(alertId);
        if (existingRecord.isPresent()) {
            AlertProcessRecord processRecord = existingRecord.get();
            processRecord.setIsFalseAlarm(true);
            processRecord.setProcessingTime(LocalDateTime.now());
            alertProcessRecordRepository.save(processRecord);
        } else {
            
            AlertProcessRecord processRecord = new AlertProcessRecord();
            processRecord.setAlertRecord(alertRecord);
            processRecord.setAlertType(alertRecord.getAlertType());
            processRecord.setIsFalseAlarm(true);
            processRecord.setProcessor("系统");
            processRecord.setProcessingTime(LocalDateTime.now());
            processRecord.setArchiveStatus("active");
            alertProcessRecordRepository.save(processRecord);
        }
        
        return alertRecordRepository.save(alertRecord);
    }
}