package com.powermanage.controller;

import com.powermanage.entity.AlertRecord;
import com.powermanage.entity.AlertProcessRecord;
import com.powermanage.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private AlertService alertService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @GetMapping
    public ResponseEntity<List<AlertRecord>> getAllAlertRecords(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            List<AlertRecord> alertRecords;
            
            
            if (username == null || username.isEmpty() || "admin".equals(username)) {
                alertRecords = alertService.getAllAlertRecords();
            } else {
                alertRecords = alertService.getAlertRecordsByUsername(username);
            }
            
            return ResponseEntity.ok(alertRecords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    @GetMapping("/unprocessed")
    public List<AlertRecord> getUnprocessedAlertRecords() {
        return alertService.getUnprocessedAlertRecords();
    }

    
    @GetMapping("/user/{userId}")
    public List<AlertRecord> getAlertRecordsByUserId(@PathVariable Long userId) {
        return alertService.getAlertRecordsByUserId(userId);
    }

    
    @GetMapping("/time-range")
    public List<AlertRecord> getAlertRecordsByTimeRange(
            @RequestParam String startTime, 
            @RequestParam String endTime) {

        LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);

        return alertService.getAlertRecordsByTimeRange(start, end);
    }

    
    @GetMapping("/severity/{severityLevel}")
    public List<AlertRecord> getAlertRecordsBySeverityLevel(@PathVariable String severityLevel) {
        return alertService.getAlertRecordsBySeverityLevel(severityLevel);
    }

    
    @PostMapping("/process/{alertId}")
    public ResponseEntity<AlertProcessRecord> processAlert(
            @PathVariable Long alertId, 
            @RequestBody AlertProcessRecord processRecord) {
        try {
            AlertProcessRecord savedRecord = alertService.processAlert(alertId, processRecord);
            return ResponseEntity.ok(savedRecord);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    
    @GetMapping("/process/{alertId}")
    public ResponseEntity<AlertProcessRecord> getAlertProcessRecordByAlertId(@PathVariable Long alertId) {
        AlertProcessRecord record = alertService.getAlertProcessRecordByAlertId(alertId);
        return record != null ? ResponseEntity.ok(record) : ResponseEntity.notFound().build();
    }

    
    @GetMapping("/process/status/{archiveStatus}")
    public List<AlertProcessRecord> getAlertProcessRecordsByArchiveStatus(@PathVariable String archiveStatus) {
        return alertService.getAlertProcessRecordsByArchiveStatus(archiveStatus);
    }

    
    @GetMapping("/report/monthly")
    public Map<String, Object> generateMonthlyAlertReport(
            @RequestParam int year, 
            @RequestParam int month) {
        return alertService.generateMonthlyAlertReport(year, month);
    }

    
    @GetMapping("/realtime")
    public List<AlertRecord> getRealtimeAlerts() {
        return alertService.getRealtimeAlerts();
    }

    
    @GetMapping("/stats")
    public Map<String, Object> getAlertStats() {
        return alertService.getAlertStats();
    }
    
    
    @PostMapping("/{alertId}/read")
    public Map<String, Object> markAsRead(@PathVariable Long alertId) {
        try {
            System.out.println("标记告警为已读，告警ID: " + alertId);
            AlertRecord alertRecord = alertService.getAlertRecordById(alertId);
            if (alertRecord != null) {
                System.out.println("找到告警记录，当前已读状态: " + alertRecord.getIsRead());
                
                alertRecord.setIsRead(true);
                AlertRecord savedRecord = alertService.saveAlertRecord(alertRecord);
                System.out.println("已读状态保存成功，保存后的状态: " + savedRecord.getIsRead());
                return Map.of("success", true);
            } else {
                System.out.println("未找到告警记录，ID: " + alertId);
                return Map.of("success", false, "message", "Alert not found");
            }
        } catch (Exception e) {
            System.out.println("标记告警已读失败: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }
    
    @PostMapping("/{alertId}/false-alarm")
    public Map<String, Object> markAsFalseAlarm(@PathVariable Long alertId) {
        try {
            System.out.println("标记告警为误报，告警ID: " + alertId);
            AlertRecord alertRecord = alertService.markAsFalseAlarm(alertId);
            return Map.of("success", true, "message", "Alert marked as false alarm successfully");
        } catch (Exception e) {
            System.out.println("标记告警为误报失败: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}