package com.powermanage.controller;

import com.powermanage.entity.AlertProcessRecord;
import com.powermanage.entity.SystemUser;
import com.powermanage.service.AlertService;
import com.powermanage.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.ServletContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.powermanage.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;



@RestController
@RequestMapping("/api/emergency")
public class EmergencyProcessingController {

    @Autowired
    private AlertService alertService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; 

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEmergencyStats() {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            List<com.powermanage.entity.AlertRecord> unprocessedAlerts = alertService.getUnprocessedAlertRecords();
            
            
            long emergencyEvents = allAlerts.stream()
                .filter(a -> "high".equals(a.getSeverityLevel()))
                .count();
            
            long processingEvents = unprocessedAlerts.size();
            
            long completedEvents = allAlerts.stream()
                .filter(a -> a.getIsProcessed())
                .count();
            
            
            String avgResponseTime = calculateAverageResponseTime(allAlerts);
            
            Map<String, Object> stats = Map.of(
                "emergencyEvents", emergencyEvents,
                "processingEvents", processingEvents,
                "completedEvents", completedEvents,
                "avgResponseTime", avgResponseTime
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping("/status-distribution")
    public ResponseEntity<List<Map<String, Object>>> getStatusDistribution() {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            
            
            long closed = allAlerts.stream()
                .filter(a -> a.getIsProcessed())
                .count();
            
            long processing = allAlerts.stream()
                .filter(a -> !a.getIsProcessed())
                .count();
            
            long completed = allAlerts.stream()
                .filter(a -> a.getIsProcessed())
                .count();
            
            long pending = allAlerts.stream()
                .filter(a -> !a.getIsProcessed())
                .count();
            
            List<Map<String, Object>> distribution = List.of(
                Map.of("name", "已关闭", "value", closed, "color", "#909399"),
                Map.of("name", "处理中", "value", processing, "color", "#e6a23c"),
                Map.of("name", "已完成", "value", completed, "color", "#67c23a"),
                Map.of("name", "未处理", "value", pending, "color", "#409eff")
            );
            
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getEmergencyEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String emergencyLevel,
            @RequestParam(required = false) String processingStatus,
            @RequestParam(required = false) String handler,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            
            
            List<com.powermanage.entity.AlertRecord> filteredAlerts = allAlerts.stream()
                .filter(alert -> eventType == null || eventType.isEmpty() || eventType.equals(alert.getAlertType()))
                .filter(alert -> emergencyLevel == null || emergencyLevel.isEmpty() || emergencyLevel.equals(alert.getSeverityLevel()))
                .filter(alert -> processingStatus == null || processingStatus.isEmpty() || 
                    (processingStatus.equals("completed") && alert.getIsProcessed()) ||
                    (processingStatus.equals("pending") && !alert.getIsProcessed()))
                .filter(alert -> handler == null || handler.isEmpty() || 
                    (alert.getProcessor() != null && alert.getProcessor().contains(handler)))
                .collect(Collectors.toList());
            
            
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, filteredAlerts.size());
            List<com.powermanage.entity.AlertRecord> pagedAlerts = filteredAlerts.subList(start, end);
            
            
            List<Map<String, Object>> events = pagedAlerts.stream()
                .map(alert -> {
                    Map<String, Object> event = new HashMap<>();
                    event.put("id", alert.getId());
                    event.put("eventId", alert.getId().toString());
                    event.put("eventType", alert.getAlertType());
                    event.put("occurTime", alert.getAlertTime() != null ? alert.getAlertTime().format(DATE_TIME_FORMATTER) : "");
                    event.put("emergencyLevel", alert.getSeverityLevel());
                    event.put("handler", alert.getProcessor() != null ? alert.getProcessor() : "未分配");
                    event.put("processingStatus", alert.getIsProcessed() ? "completed" : "pending");
                    event.put("description", alert.getAlertContent() != null ? alert.getAlertContent() : "无描述");
                    event.put("isProcessed", alert.getIsProcessed());
                    return event;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", events);
            result.put("total", filteredAlerts.size());
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) filteredAlerts.size() / pageSize));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping("/events/{eventId}")
    public ResponseEntity<Map<String, Object>> getEmergencyEventDetails(@PathVariable String eventId) {
        try {
            
            Long alertId;
            try {
                alertId = Long.parseLong(eventId);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(400).body(Map.of("error", "无效的事件ID"));
            }
            
            
            com.powermanage.entity.AlertRecord alert;
            try {
                alert = alertService.getAlertRecordById(alertId);
            } catch (RuntimeException e) {
                return ResponseEntity.status(404).body(Map.of("error", "未找到事件"));
            }
            
            
            Map<String, Object> details = new HashMap<>();
            details.put("eventId", eventId);
            details.put("id", alertId);
            details.put("eventType", alert.getAlertType());
            details.put("occurTime", alert.getAlertTime() != null ? alert.getAlertTime().format(DATE_TIME_FORMATTER) : "");
            details.put("emergencyLevel", alert.getSeverityLevel());
            details.put("handler", alert.getProcessor() != null ? alert.getProcessor() : "未分配");
            details.put("processingStatus", alert.getIsProcessed() ? "completed" : "pending");
            details.put("description", alert.getAlertContent() != null ? alert.getAlertContent() : "无描述");
            details.put("isProcessed", alert.getIsProcessed());
            
            
            String location = "未知位置";
            int affectedUsers = 1; 
            
            
            if (alert.getUser() != null) {
                if (alert.getUser().getCommunity() != null) {
                    location = alert.getUser().getCommunity();
                }
                
                String community = alert.getUser().getCommunity();
                if (community != null) {
                    
                    affectedUsers = 1; 
                }
            }
            
            details.put("location", location);
            details.put("affectedUsers", affectedUsers);
            
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/events/{eventId}/process")
    public ResponseEntity<Map<String, Object>> processEmergencyEvent(
            @PathVariable String eventId,
            @RequestBody(required = false) Map<String, Object> processInfo) {
        try {
            
            Long alertId;
            try {
                alertId = Long.parseLong(eventId);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(400).body(Map.of("error", "无效的事件ID"));
            }
            
            
            com.powermanage.entity.AlertRecord alert;
            try {
                alert = alertService.getAlertRecordById(alertId);
            } catch (RuntimeException e) {
                return ResponseEntity.status(404).body(Map.of("error", "未找到事件"));
            }
            
            
            AlertProcessRecord existingRecord = alertService.getAlertProcessRecordByAlertId(alertId);
            AlertProcessRecord savedRecord;
            
            if (existingRecord != null) {
                
                existingRecord.setProcessor(processInfo != null && processInfo.get("processor") != null ? 
                    processInfo.get("processor").toString() : "系统管理员");
                existingRecord.setProcessingTime(LocalDateTime.now());
                existingRecord.setProcessingResult(processInfo != null && processInfo.get("result") != null ? 
                    processInfo.get("result").toString() : "事件已处理");
                existingRecord.setArchiveStatus("已处理");
                
                savedRecord = alertService.closeAlert(alertId, existingRecord);
            } else {
                
                AlertProcessRecord record = new AlertProcessRecord();
                record.setAlertRecord(alert);
                record.setAlertType(alert.getAlertType());
                record.setProcessor(processInfo != null && processInfo.get("processor") != null ? 
                    processInfo.get("processor").toString() : "系统管理员");
                record.setProcessingTime(LocalDateTime.now());
                record.setProcessingResult(processInfo != null && processInfo.get("result") != null ? 
                    processInfo.get("result").toString() : "事件已处理");
                record.setArchiveStatus("已处理");
                
                savedRecord = alertService.processAlert(alertId, record);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "事件处理成功");
            result.put("processRecord", savedRecord);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/events/{eventId}/close")
    public ResponseEntity<Map<String, Object>> closeEmergencyEvent(
            @PathVariable String eventId,
            @RequestBody(required = false) Map<String, Object> closeInfo) {
        try {
            
            Long alertId;
            try {
                alertId = Long.parseLong(eventId);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(400).body(Map.of("error", "无效的事件ID"));
            }
            
            
            com.powermanage.entity.AlertRecord alert;
            try {
                alert = alertService.getAlertRecordById(alertId);
            } catch (RuntimeException e) {
                return ResponseEntity.status(404).body(Map.of("error", "未找到事件"));
            }
            
            
            AlertProcessRecord record = new AlertProcessRecord();
            record.setAlertRecord(alert);
            record.setAlertType(alert.getAlertType());
            record.setProcessor(closeInfo != null && closeInfo.get("processor") != null ? 
                closeInfo.get("processor").toString() : "系统管理员");
            record.setProcessingTime(LocalDateTime.now());
            record.setProcessingResult(closeInfo != null && closeInfo.get("result") != null ? 
                closeInfo.get("result").toString() : "事件已关闭");
            record.setArchiveStatus("已关闭");
            
            
            AlertProcessRecord savedRecord = alertService.closeAlert(alertId, record);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "事件关闭成功");
            result.put("processRecord", savedRecord);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    
    @PostMapping("/start-response")
    public ResponseEntity<Map<String, Object>> startEmergencyResponse(@RequestBody Map<String, Object> payload) {
        try {
            
            
            
            
            String eventId = (String) payload.getOrDefault("eventId", "none");
            
            
            System.out.println("启动应急响应，事件ID: " + eventId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "应急响应已成功启动");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/contact-personnel")
    public ResponseEntity<Map<String, Object>> contactPersonnel(@RequestBody Map<String, Object> payload) {
        try {
            List<?> personnelList = (List<?>) payload.get("personnel");
            String message = (String) payload.get("message");

            
            for (Object person : personnelList) {
                
                String recipientId = person.toString();
                
                
                if (recipientId.matches("\\d+")) {
                    
                    Optional<SystemUser> userOptional = systemUserRepository.findById(Long.parseLong(recipientId));
                    if (userOptional.isPresent()) {
                        recipientId = userOptional.get().getUsername();
                    } else {
                        
                        recipientId = "admin";
                    }
                }
                
                
                notificationService.sendNotification(recipientId, message);
                
                
                Map<String, Object> wsMessage = new HashMap<>();
                wsMessage.put("type", "urgent_alert");
                wsMessage.put("message", message);
                wsMessage.put("timestamp", LocalDateTime.now().format(DATE_TIME_FORMATTER));
                wsMessage.put("sender", "应急处理系统");
                
                
                messagingTemplate.convertAndSendToUser(recipientId, "/queue/user-alerts", wsMessage);
                
                System.out.println("WebSocket消息已发送给用户: " + recipientId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "通知已发送");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/generate-report")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody Map<String, Object> payload) {
        try {
            
            Object eventIdObj = payload.get("eventId");
            String eventId;
            
            if (eventIdObj == null) {
                eventId = "none";
            } else if (eventIdObj instanceof String) {
                eventId = (String) eventIdObj;
            } else {
                
                eventId = String.valueOf(eventIdObj);
            }
            
            System.out.println("为事件ID生成报告: " + eventId);
            
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            
            
            String reportContent = generateReportContent(eventId);
            
            
            
            String reportId = "report_" + timestamp;
            reportStorage.put(reportId, reportContent);
            
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "报告已生成");
            
            result.put("reportUrl", "/api/emergency/download-report?reportId=" + reportId);
            result.put("reportId", reportId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "报告生成失败: " + e.getMessage()));
        }
    }
    
    
    private static final Map<String, String> reportStorage = new HashMap<>();
    
    
    private String generateReportContent(String eventId) {
        StringBuilder content = new StringBuilder();
        content.append("应急处理报告\n");
        content.append("================\n\n");
        content.append("生成时间: ").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("\n");
        content.append("事件ID: ").append(eventId).append("\n\n");
        
        
        if (!"none".equals(eventId)) {
            try {
                Long alertId = Long.parseLong(eventId);
                com.powermanage.entity.AlertRecord alert = alertService.getAlertRecordById(alertId);
                
                content.append("事件详情:\n");
                content.append("--------\n");
                content.append("事件类型: ").append(alert.getAlertType()).append("\n");
                content.append("严重程度: ").append(alert.getSeverityLevel()).append("\n");
                content.append("发生时间: ").append(alert.getAlertTime() != null ? 
                    alert.getAlertTime().format(DATE_TIME_FORMATTER) : "未知").append("\n");
                content.append("处理状态: ").append(alert.getIsProcessed() ? "已处理" : "未处理").append("\n");
                content.append("负责人: ").append(alert.getProcessor() != null ? alert.getProcessor() : "未分配").append("\n");
                content.append("事件描述: ").append(alert.getAlertContent() != null ? alert.getAlertContent() : "无描述").append("\n\n");
                
                
                AlertProcessRecord processRecord = alertService.getAlertProcessRecordByAlertId(alertId);
                if (processRecord != null) {
                    content.append("处理记录:\n");
                    content.append("--------\n");
                    content.append("处理人: ").append(processRecord.getProcessor()).append("\n");
                    content.append("处理时间: ").append(processRecord.getProcessingTime() != null ? 
                        processRecord.getProcessingTime().format(DATE_TIME_FORMATTER) : "未知").append("\n");
                    content.append("处理结果: ").append(processRecord.getProcessingResult()).append("\n");
                    content.append("归档状态: ").append(processRecord.getArchiveStatus()).append("\n\n");
                }
            } catch (Exception e) {
                content.append("无法获取事件详情: ").append(e.getMessage()).append("\n\n");
            }
        } else {
            content.append("综合报告:\n");
            content.append("--------\n");
            
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            long totalEvents = allAlerts.size();
            long emergencyEvents = allAlerts.stream()
                .filter(a -> "high".equals(a.getSeverityLevel()))
                .count();
            long processedEvents = allAlerts.stream()
                .filter(a -> a.getIsProcessed())
                .count();
            long unprocessedEvents = totalEvents - processedEvents;
            
            content.append("总事件数: ").append(totalEvents).append("\n");
            content.append("紧急事件数: ").append(emergencyEvents).append("\n");
            content.append("已处理事件数: ").append(processedEvents).append("\n");
            content.append("未处理事件数: ").append(unprocessedEvents).append("\n\n");
            
            
            content.append("处理建议:\n");
            content.append("--------\n");
            if (unprocessedEvents > 0) {
                content.append("- 有 ").append(unprocessedEvents).append(" 个事件尚未处理，请优先处理高紧急程度事件\n");
            }
            if (emergencyEvents > 0) {
                content.append("- 有 ").append(emergencyEvents).append(" 个高紧急程度事件，需要立即关注\n");
            }
            content.append("- 建议定期检查系统状态，预防潜在问题\n");
        }
        
        content.append("\n报告生成结束\n");
        return content.toString();
    }

    
    @PostMapping("/save-settings")
    public ResponseEntity<Map<String, Object>> saveSettings(@RequestBody Map<String, Object> settings) {
        try {
            System.out.println("保存应急设置: " + settings);
            
            
            
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "设置已保存");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        try {
            
            
            Map<String, Object> settings = new HashMap<>();
            settings.put("autoStartResponse", false);
            settings.put("defaultPersonnel", new ArrayList<>());
            settings.put("reportTemplate", "standard");
            
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/download-report")
    public ResponseEntity<byte[]> downloadReport(@RequestParam String reportId) {
        try {
            
            String reportContent = reportStorage.get(reportId);
            if (reportContent == null) {
                
                String eventId = reportId.replace("report_", "");
                if (!"none".equals(eventId)) {
                    reportContent = generateReportContent(eventId);
                    reportStorage.put(reportId, reportContent);
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            
            
            byte[] content = reportContent.getBytes("UTF-8");
            
            
            String filename = "EmergencyReport_" + reportId.replace("report_", "") + ".txt";
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .body(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    
    @GetMapping("/event-types")
    public ResponseEntity<List<Map<String, Object>>> getEventTypes() {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            
            
            List<String> distinctTypes = allAlerts.stream()
                .map(com.powermanage.entity.AlertRecord::getAlertType)
                .filter(type -> type != null && !type.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            
            List<Map<String, Object>> eventTypes = distinctTypes.stream()
                .map(type -> {
                    Map<String, Object> itemType = new HashMap<>();
                    itemType.put("value", type);
                    itemType.put("label", getEventTypeName(type));
                    return itemType;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(eventTypes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/emergency-levels")
    public ResponseEntity<List<Map<String, Object>>> getEmergencyLevels() {
        try {
            
            List<com.powermanage.entity.AlertRecord> allAlerts = alertService.getAllAlertRecords();
            
            
            List<String> distinctLevels = allAlerts.stream()
                .map(com.powermanage.entity.AlertRecord::getSeverityLevel)
                .filter(level -> level != null && !level.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            
            List<Map<String, Object>> emergencyLevels = distinctLevels.stream()
                .map(level -> {
                    Map<String, Object> itemLevel = new HashMap<>();
                    itemLevel.put("value", level);
                    itemLevel.put("label", getEmergencyLevelName(level));
                    return itemLevel;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(emergencyLevels);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/processing-statuses")
    public ResponseEntity<List<Map<String, Object>>> getProcessingStatuses() {
        try {
            
            List<Map<String, Object>> processingStatuses = List.of(
                Map.of("value", "completed", "label", "已完成"),
                Map.of("value", "pending", "label", "未处理"),
                Map.of("value", "processing", "label", "处理中"),
                Map.of("value", "closed", "label", "已关闭")
            );
            
            return ResponseEntity.ok(processingStatuses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    
    private String getEventTypeName(String type) {
        if (type == null) return "";
        
        switch (type) {
            case "power outage":
                return "电力中断";
            case "equipment failure":
                return "设备故障";
            case "voltage anomaly":
                return "电压异常";
            case "current anomaly":
                return "电流异常";
            case "temperature anomaly":
                return "温度异常";
            case "power quality issue":
                return "电能质量问题";
            default:
                return type;
        }
    }

    
    private String getEmergencyLevelName(String level) {
        if (level == null) return "";
        
        switch (level) {
            case "high":
                return "一级";
            case "medium":
                return "二级";
            case "low":
                return "三级";
            default:
                return level;
        }
    }

    
    private String calculateAverageResponseTime(List<com.powermanage.entity.AlertRecord> alerts) {
        try {
            List<com.powermanage.entity.AlertRecord> processedAlerts = alerts.stream()
                .filter(alert -> alert.getIsProcessed() && alert.getProcessTime() != null && alert.getAlertTime() != null)
                .collect(Collectors.toList());
            
            if (processedAlerts.isEmpty()) {
                return "0min";
            }
            
            
            double totalMinutes = processedAlerts.stream()
                .mapToLong(alert -> java.time.Duration.between(alert.getAlertTime(), alert.getProcessTime()).toMinutes())
                .average()
                .orElse(0.0);
            
            return String.format("%.0fmin", totalMinutes);
        } catch (Exception e) {
            System.err.println("计算平均响应时间失败: " + e.getMessage());
            return "0min";
        }
    }
}
