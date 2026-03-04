package com.powermanage.controller;

import com.powermanage.entity.AnomalyDetectionRecord;
import com.powermanage.entity.User;
import com.powermanage.repository.AnomalyDetectionRecordRepository;
import com.powermanage.service.AnomalyDetectionService;
import com.powermanage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/anomaly-records")
public class AnomalyRecordsController {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private AnomalyDetectionRecordRepository anomalyDetectionRecordRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAnomalyStatistics() {
        try {
            
            List<AnomalyDetectionRecord> allRecords = anomalyDetectionRecordRepository.findByDetectionTimeBetweenWithUser(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());
            
            
            long todayNew = allRecords.stream()
                .filter(r -> r.getDetectionTime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
            
            long todayProcessed = allRecords.stream()
                .filter(r -> r.getIsHandled() && r.getProcessTime() != null && 
                        r.getProcessTime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
            
            long unprocessed = allRecords.stream()
                .filter(r -> !r.getIsHandled())
                .count();
            
            long highSeverity = allRecords.stream()
                .filter(r -> "high".equals(determineSeverityLevel(r.getAnomalyScore())))
                .count();
            
            long mediumSeverity = allRecords.stream()
                .filter(r -> "medium".equals(determineSeverityLevel(r.getAnomalyScore())))
                .count();
            
            long lowSeverity = allRecords.stream()
                .filter(r -> "low".equals(determineSeverityLevel(r.getAnomalyScore())))
                .count();
            
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("todayNew", todayNew);
            stats.put("todayProcessed", todayProcessed);
            stats.put("processedToday", todayProcessed); 
            stats.put("unprocessed", unprocessed);
            stats.put("highSeverity", highSeverity);
            stats.put("highRisk", highSeverity); 
            stats.put("mediumSeverity", mediumSeverity);
            stats.put("mediumRisk", mediumSeverity); 
            stats.put("lowSeverity", lowSeverity);
            stats.put("lowRisk", lowSeverity); 
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAnomalyRecords(
            @RequestParam(required = false) String anomalyId,
            @RequestParam(required = false) String userInfo,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String severityLevel,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String processStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            
            List<AnomalyDetectionRecord> allRecords = anomalyDetectionRecordRepository.findByDetectionTimeBetweenWithUser(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());
            
            
            List<AnomalyDetectionRecord> filteredRecords = allRecords.stream()
                .filter(record -> {
                    
                    if (anomalyId != null && !anomalyId.isEmpty() && 
                        !record.getId().toString().contains(anomalyId)) {
                        return false;
                    }
                    
                    
                    if (userInfo != null && !userInfo.isEmpty()) {
                        boolean match = record.getUserId().toString().contains(userInfo);
                        
                        if (!match && record.getUser() != null && record.getUser().getName() != null) {
                            match = record.getUser().getName().contains(userInfo);
                        }
                        
                        if (!match) {
                            List<User> users = userService.getUsersByName(userInfo);
                            match = users.stream().anyMatch(user -> user.getId().equals(record.getUserId()));
                        }
                        
                        if (!match) {
                            return false;
                        }
                    }
                    
                    
                    if (anomalyType != null && !anomalyType.isEmpty() && 
                        !anomalyType.equals(translateAnomalyType(record.getAnomalyType()))) {
                        return false;
                    }
                    
                    
                    if (severityLevel != null && !severityLevel.isEmpty() && 
                        !severityLevel.equals(determineSeverityLevel(record.getAnomalyScore()))) {
                        return false;
                    }
                    
                    
                    if (processStatus != null && !processStatus.isEmpty()) {
                        boolean isProcessed = record.getIsHandled();
                        if ("processed".equals(processStatus) && !isProcessed) {
                            return false;
                        }
                        if ("unprocessed".equals(processStatus) && isProcessed) {
                            return false;
                        }
                    }
                    
                    
                    if (timeRange != null && !timeRange.isEmpty()) {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime startTime = null;
                        
                        switch (timeRange) {
                            case "today":
                                startTime = now.toLocalDate().atStartOfDay();
                                break;
                            case "week":
                                startTime = now.minusDays(7);
                                break;
                            case "month":
                                startTime = now.minusMonths(1);
                                break;
                        }
                        
                        if (startTime != null && record.getDetectionTime() != null && 
                            record.getDetectionTime().isBefore(startTime)) {
                            return false;
                        }
                    }
                    
                    
                    if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                        try {
                            LocalDateTime start = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
                            LocalDateTime end = LocalDateTime.parse(endDate, DATE_TIME_FORMATTER);
                            
                            if (record.getDetectionTime() != null && 
                                (record.getDetectionTime().isBefore(start) || record.getDetectionTime().isAfter(end))) {
                                return false;
                            }
                        } catch (Exception e) {
                            
                        }
                    }
                    
                    return true;
                })
                .sorted((r1, r2) -> {
                    // 按异常ID降序排序，ID越大越新
                    return r2.getId().compareTo(r1.getId());
                })
                .collect(Collectors.toList());
            
            
            int total = filteredRecords.size();
            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            
            List<AnomalyDetectionRecord> pagedRecords = filteredRecords.subList(fromIndex, toIndex);
            
            
            List<Map<String, Object>> records = pagedRecords.stream()
                .map(record -> {
                    Map<String, Object> anomalyRecord = new HashMap<>();
                    anomalyRecord.put("id", record.getId().toString());
                    anomalyRecord.put("userId", record.getUserId().toString());
                    String userName = "未知用户";
                    String userAddress = "未知地址";
                    
                    // 优先通过 userId 从数据库获取完整的用户信息
                    if (record.getUserId() != null) {
                        Optional<User> userOpt = userService.getUserById(record.getUserId());
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            userName = user.getName() != null ? user.getName() : "未知用户";
                            userAddress = user.getAddress() != null ? user.getAddress() : "未知地址";
                        } else {
                            // 如果通过 ID 找不到，尝试使用 record 中的 User 对象
                            if (record.getUser() != null) {
                                userName = record.getUser().getName() != null ? record.getUser().getName() : "用户" + record.getUserId();
                                userAddress = record.getUser().getAddress() != null ? record.getUser().getAddress() : "未知地址";
                            } else {
                                userName = "用户" + record.getUserId();
                            }
                        }
                    }
                    
                    anomalyRecord.put("userName", userName);
                    anomalyRecord.put("userAddress", userAddress);
                    anomalyRecord.put("anomalyType", translateAnomalyType(record.getAnomalyType()));
                    anomalyRecord.put("detectionTime", record.getDetectionTime().format(DATE_TIME_FORMATTER));
                    anomalyRecord.put("description", record.getAnomalyDetails() != null ? record.getAnomalyDetails() : "无描述");
                    anomalyRecord.put("severityLevel", determineSeverityLevel(record.getAnomalyScore()));
                    anomalyRecord.put("isProcessed", record.getIsHandled());
                    anomalyRecord.put("isFalseAlarm", record.getIsFalseAlarm() != null ? record.getIsFalseAlarm() : false);
                    anomalyRecord.put("processTime", record.getProcessTime() != null ? 
                            record.getProcessTime().format(DATE_TIME_FORMATTER) : ""); 
                    anomalyRecord.put("processor", record.getProcessor() != null ? record.getProcessor() : ""); 
                    return anomalyRecord;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/process/{id}")
    public ResponseEntity<Map<String, Object>> processAnomalyRecord(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> processInfo) {
        
        try {
            
            Optional<AnomalyDetectionRecord> recordOpt = anomalyDetectionRecordRepository.findById(id);
            
            if (!recordOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "未找到异常记录"));
            }
            
            AnomalyDetectionRecord record = recordOpt.get();
            
            
            if (record.getIsHandled()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "此异常记录已经处理过了",
                    "id", record.getId(),
                    "isProcessed", true
                ));
            }
            
            
            record.setIsHandled(true);
            record.setProcessTime(LocalDateTime.now());
            record.setProcessor(processInfo != null ? processInfo.getOrDefault("processor", "系统管理员") : "系统管理员");
            
            
            anomalyDetectionRecordRepository.save(record);
            
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "异常记录处理成功");
            result.put("id", record.getId());
            result.put("isProcessed", record.getIsHandled());
            result.put("processTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "处理异常记录失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/false-alarm")
    public ResponseEntity<Map<String, Object>> markAsFalseAlarm(@PathVariable Long id) {
        try {
            System.out.println("标记异常记录为误报，记录ID: " + id);
            Optional<AnomalyDetectionRecord> recordOpt = anomalyDetectionRecordRepository.findById(id);
            
            if (!recordOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "异常记录未找到"));
            }
            
            AnomalyDetectionRecord record = recordOpt.get();
            
            
            record.setIsFalseAlarm(true);
            record.setIsHandled(true);
            record.setProcessTime(LocalDateTime.now());
            record.setProcessor("系统管理员");
            
            anomalyDetectionRecordRepository.save(record);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "异常记录已标记为误报"));
        } catch (Exception e) {
            System.out.println("标记异常记录为误报失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    
    @PostMapping("/batch-process")
    public ResponseEntity<Map<String, Object>> batchProcessAnomalyRecords(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) request.get("ids");
            String processor = (String) request.getOrDefault("processor", "系统管理员");
            
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请提供要处理的异常记录ID列表"));
            }
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Long id : ids) {
                try {
                    
                    Optional<AnomalyDetectionRecord> recordOpt = anomalyDetectionRecordRepository.findById(id);
                    
                    if (recordOpt.isPresent()) {
                        AnomalyDetectionRecord record = recordOpt.get();
                        
                        record.setIsHandled(true);
                        record.setProcessTime(LocalDateTime.now());
                        record.setProcessor(processor);
                        anomalyDetectionRecordRepository.save(record);
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    failureCount++;
                }
            }
            
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量处理完成");
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("totalCount", ids.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "批量处理异常记录失败: " + e.getMessage()));
        }
    }

    
    @GetMapping("/export")
    public ResponseEntity<String> exportAnomalyRecords(
            @RequestParam(required = false) String anomalyId,
            @RequestParam(required = false) String userInfo,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String severityLevel,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String processStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            
            List<AnomalyDetectionRecord> allRecords = anomalyDetectionRecordRepository.findByDetectionTimeBetweenWithUser(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());
            
            
            List<AnomalyDetectionRecord> filteredRecords = allRecords.stream()
                .filter(record -> {
                    
                    if (anomalyId != null && !anomalyId.isEmpty() && 
                        !record.getId().toString().contains(anomalyId)) {
                        return false;
                    }
                    
                    
                    if (userInfo != null && !userInfo.isEmpty()) {
                        boolean match = record.getUserId().toString().contains(userInfo);
                        
                        if (!match && record.getUser() != null && record.getUser().getName() != null) {
                            match = record.getUser().getName().contains(userInfo);
                        }
                        
                        if (!match) {
                            List<User> users = userService.getUsersByName(userInfo);
                            match = users.stream().anyMatch(user -> user.getId().equals(record.getUserId()));
                        }
                        
                        if (!match) {
                            return false;
                        }
                    }
                    
                    
                    if (anomalyType != null && !anomalyType.isEmpty() && 
                        !anomalyType.equals(translateAnomalyType(record.getAnomalyType()))) {
                        return false;
                    }
                    
                    
                    if (severityLevel != null && !severityLevel.isEmpty() && 
                        !severityLevel.equals(determineSeverityLevel(record.getAnomalyScore()))) {
                        return false;
                    }
                    
                    
                    if (processStatus != null && !processStatus.isEmpty()) {
                        boolean isProcessed = record.getIsHandled();
                        if ("processed".equals(processStatus) && !isProcessed) {
                            return false;
                        }
                        if ("unprocessed".equals(processStatus) && isProcessed) {
                            return false;
                        }
                    }
                    
                    
                    if (timeRange != null && !timeRange.isEmpty()) {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime startTime = null;
                        
                        switch (timeRange) {
                            case "today":
                                startTime = now.toLocalDate().atStartOfDay();
                                break;
                            case "week":
                                startTime = now.minusDays(7);
                                break;
                            case "month":
                                startTime = now.minusMonths(1);
                                break;
                        }
                        
                        if (startTime != null && record.getDetectionTime() != null && 
                            record.getDetectionTime().isBefore(startTime)) {
                            return false;
                        }
                    }
                    
                    
                    if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                        try {
                            LocalDateTime start = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
                            LocalDateTime end = LocalDateTime.parse(endDate, DATE_TIME_FORMATTER);
                            
                            if (record.getDetectionTime() != null && 
                                (record.getDetectionTime().isBefore(start) || record.getDetectionTime().isAfter(end))) {
                                return false;
                            }
                        } catch (Exception e) {
                            
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
            
            
            StringBuilder exportData = new StringBuilder();
            
            
            exportData.append("异常ID,用户ID,用户名,异常类型,检测时间,描述,严重程度,处理状态,处理时间,处理人\n");
            
            
            for (AnomalyDetectionRecord record : filteredRecords) {
                exportData.append(record.getId()).append(",");
                exportData.append(record.getUserId()).append(",");
                String userName = "未知用户";
                if (record.getUser() != null && record.getUser().getName() != null) {
                    userName = record.getUser().getName();
                } else if (record.getUserId() != null) {
                    userName = userService.getUserById(record.getUserId())
                            .map(user -> user.getName())
                            .orElse("用户" + record.getUserId());
                }
                exportData.append(userName).append(",");
                exportData.append(translateAnomalyType(record.getAnomalyType())).append(",");
                exportData.append(record.getDetectionTime() != null ? 
                    record.getDetectionTime().format(DATE_TIME_FORMATTER) : "").append(",");
                exportData.append(record.getAnomalyDetails() != null ? record.getAnomalyDetails() : "无描述").append(",");
                exportData.append(determineSeverityLevel(record.getAnomalyScore())).append(",");
                exportData.append(record.getIsHandled() ? "已处理" : "未处理").append(",");
                exportData.append("").append(","); 
                exportData.append("").append("\n"); 
            }
            
            return ResponseEntity.ok(exportData.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("导出失败: " + e.getMessage());
        }
    }

    
    private String translateAnomalyType(String type) {
        if (type == null) return "未知";
        
        switch (type) {
            case "HIGH_DEVIATION": return "高峰异常";
            case "PATTERN_DEVIATION": return "模式异常";
            case "TREND_DEVIATION": return "趋势异常";
            default: return type;
        }
    }

    
    private String determineSeverityLevel(Double score) {
        if (score == null) return "low";
        
        if (score > 0.8) return "high";
        if (score > 0.5) return "medium";
        return "low";
    }
}
