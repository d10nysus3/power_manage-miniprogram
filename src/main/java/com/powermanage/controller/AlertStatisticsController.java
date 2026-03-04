package com.powermanage.controller;

import com.powermanage.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alert-stats")
public class AlertStatisticsController {

    @Autowired
    private AlertService alertService;

    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            List<com.powermanage.entity.AlertRecord> unprocessedAlerts = alertService.getUnprocessedAlertRecords();
            
            
            long highRisk = allAlerts.stream()
                .filter(a -> "high".equals(a.getSeverityLevel()))
                .count();
            
            long mediumRisk = allAlerts.stream()
                .filter(a -> "medium".equals(a.getSeverityLevel()))
                .count();
            
            long lowRisk = allAlerts.stream()
                .filter(a -> "low".equals(a.getSeverityLevel()))
                .count();
            
            long processedToday = allAlerts.stream()
                .filter(a -> a.getProcessTime() != null && 
                    a.getProcessTime().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
            
            Map<String, Object> stats = Map.of(
                "total", allAlerts.size(),
                "highRisk", highRisk,
                "mediumRisk", mediumRisk,
                "lowRisk", lowRisk,
                "processedToday", processedToday,
                "unprocessed", unprocessedAlerts.size()
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlertRecords(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String processStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            
            
            List<Map<String, Object>> records = allAlerts.stream()
                .map(alert -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("alertId", alert.getId().toString());
                    record.put("userId", alert.getUser().getId().toString());
                    record.put("username", alert.getUser().getName());
                    record.put("anomalyType", alert.getAlertType());
                    record.put("severityLevel", alert.getSeverityLevel());
                    record.put("alertTime", alert.getAlertTime().toString());
                    record.put("processingStatus", alert.getIsProcessed() ? "completed" : "pending");
                    return record;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", records.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping("/realtime")
    public List<com.powermanage.entity.AlertRecord> getRealtimeAlerts() {
        return alertService.getRealtimeAlerts();
    }

    
    @GetMapping("/unprocessed")
    public List<com.powermanage.entity.AlertRecord> getUnprocessedAlertRecords() {
        return alertService.getUnprocessedAlertRecords();
    }

    
    @GetMapping("/user/{userId}")
    public List<com.powermanage.entity.AlertRecord> getAlertRecordsByUserId(@PathVariable Long userId) {
        return alertService.getAlertRecordsByUserId(userId);
    }

    
    @GetMapping("/time-range")
    public List<com.powermanage.entity.AlertRecord> getAlertRecordsByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime, 
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime, 
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return alertService.getAlertRecordsByTimeRange(start, end);
    }

    
    @GetMapping("/severity/{severityLevel}")
    public List<com.powermanage.entity.AlertRecord> getAlertRecordsBySeverityLevel(@PathVariable String severityLevel) {
        return alertService.getAlertRecordsBySeverityLevel(severityLevel);
    }

    
    @PostMapping("/process/{alertId}")
    public ResponseEntity<com.powermanage.entity.AlertProcessRecord> processAlert(
            @PathVariable Long alertId, 
            @RequestBody com.powermanage.entity.AlertProcessRecord processRecord) {
        try {
            com.powermanage.entity.AlertProcessRecord savedRecord = alertService.processAlert(alertId, processRecord);
            return ResponseEntity.ok(savedRecord);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/process/{alertId}")
    public ResponseEntity<com.powermanage.entity.AlertProcessRecord> getAlertProcessRecordByAlertId(@PathVariable Long alertId) {
        com.powermanage.entity.AlertProcessRecord record = alertService.getAlertProcessRecordByAlertId(alertId);
        return record != null ? ResponseEntity.ok(record) : ResponseEntity.notFound().build();
    }

    
    @GetMapping("/process/status/{archiveStatus}")
    public List<com.powermanage.entity.AlertProcessRecord> getAlertProcessRecordsByArchiveStatus(@PathVariable String archiveStatus) {
        return alertService.getAlertProcessRecordsByArchiveStatus(archiveStatus);
    }

    
    @GetMapping("/report/monthly")
    public Map<String, Object> generateMonthlyAlertReport(
            @RequestParam int year, 
            @RequestParam int month) {
        return alertService.generateMonthlyAlertReport(year, month);
    }
}
