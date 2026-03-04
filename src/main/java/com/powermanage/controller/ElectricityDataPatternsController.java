package com.powermanage.controller;

import com.powermanage.entity.UserElectricityPattern;
import com.powermanage.service.AnomalyDetectionService;
import com.powermanage.service.UserService;
import com.powermanage.service.ElectricityDataService;
import com.powermanage.entity.User;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/electricity-data")
public class ElectricityDataPatternsController {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private ElectricityDataService electricityDataService;

    @Autowired
    private UserService userService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @GetMapping("/patterns")
    public ResponseEntity<List<Map<String, Object>>> getElectricityPatterns(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "30") String period) {
        try {
            List<Map<String, Object>> patterns = new ArrayList<>();
            
            if (userId != null) {
                
                UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, "2.0");
                if (pattern == null) {
                     pattern = anomalyDetectionService.getUserElectricityPattern(userId, "1.0");
                }
                
                if (pattern != null) {
                    Map<String, Object> patternData = convertPatternToMap(pattern);
                    patterns.add(patternData);
                } else {
                    
                    Map<String, Object> patternData = analyzeUserElectricityPattern(userId, Integer.parseInt(period));
                    patterns.add(patternData);
                }
            } else {
                // 如果没有提供 userId，获取所有用户的分析数据
                List<User> allUsers = userService.getAllUsers();
                // 为避免数据量过大，这里限制返回前 50 个用户，或者应该支持分页
                // 考虑到演示环境，暂不分页，但限制数量
                allUsers.stream().limit(50).forEach(user -> {
                    try {
                        UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(user.getId(), "2.0");
                         if (pattern == null) {
                             pattern = anomalyDetectionService.getUserElectricityPattern(user.getId(), "1.0");
                        }
                        
                        if (pattern != null) {
                            patterns.add(convertPatternToMap(pattern));
                        } else {
                            patterns.add(analyzeUserElectricityPattern(user.getId(), Integer.parseInt(period)));
                        }
                    } catch (Exception e) {
                        // 忽略单个用户的错误
                        System.err.println("Error processing user " + user.getId() + ": " + e.getMessage());
                    }
                });
            }
            
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/patterns/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getElectricityPatternsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "30") String period) {
        try {
            List<Map<String, Object>> patterns = new ArrayList<>();
            
            
            UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, "1.0");
            if (pattern != null) {
                Map<String, Object> patternData = convertPatternToMap(pattern);
                patterns.add(patternData);
            } else {
                
                Map<String, Object> patternData = analyzeUserElectricityPattern(userId, Integer.parseInt(period));
                patterns.add(patternData);
            }
            
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @PostMapping("/generate-patterns-report")
    public ResponseEntity<Map<String, Object>> generateAnalysisReport(@RequestBody Map<String, Object> request) {
        try {
            Long userId = (Long) request.get("userId");
            String period = (String) request.get("period");
            String reportType = (String) request.get("reportType");
            
            
            String reportId = "RPT-" + System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();
            
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportId", reportId);
            reportData.put("userId", userId);
            reportData.put("reportType", reportType != null ? reportType : "standard");
            reportData.put("period", period != null ? period : "30");
            reportData.put("generateTime", now.format(DATE_TIME_FORMATTER));
            reportData.put("status", "completed");
            
            
            if (userId != null) {
                UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, "1.0");
                if (pattern != null) {
                    reportData.put("analysis", convertPatternToMap(pattern));
                } else {
                    reportData.put("analysis", analyzeUserElectricityPattern(userId, Integer.parseInt(period != null ? period : "30")));
                }
            }
            
            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping("/export-patterns")
    public ResponseEntity<String> exportAnalysisData(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "30") String period,
            @RequestParam(required = false, defaultValue = "excel") String format) {
        try {
            StringBuilder exportData = new StringBuilder();
            
            if ("csv".equals(format.toLowerCase())) {
                
                exportData.append("用户ID,用户名,平均日用电量,高峰时段,模式类型,预测月用电量,分析日期\n");
                
                if (userId != null) {
                    
                    UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, "1.0");
                    if (pattern != null) {
                        Map<String, Object> patternData = convertPatternToMap(pattern);
                        exportData.append(patternData.get("userId")).append(",");
                        exportData.append(patternData.get("username")).append(",");
                        exportData.append(patternData.get("averageDailyUsage")).append(",");
                        exportData.append(patternData.get("peakHour")).append(",");
                        exportData.append(patternData.get("patternType")).append(",");
                        exportData.append(patternData.get("predictedMonthlyUsage")).append(",");
                        exportData.append(patternData.get("analysisDate")).append("\n");
                    } else {
                        Map<String, Object> patternData = analyzeUserElectricityPattern(userId, Integer.parseInt(period));
                        exportData.append(patternData.get("userId")).append(",");
                        exportData.append(patternData.get("username")).append(",");
                        exportData.append(patternData.get("averageDailyUsage")).append(",");
                        exportData.append(patternData.get("peakHour")).append(",");
                        exportData.append(patternData.get("patternType")).append(",");
                        exportData.append(patternData.get("predictedMonthlyUsage")).append(",");
                        exportData.append(patternData.get("analysisDate")).append("\n");
                    }
                } else {
                    
                    exportData.append("1,示例用户,25.6,19:00-22:00,住宅型,780,").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("\n");
                }
            } else {
                
                exportData.append("{\n");
                exportData.append("  \"format\": \"excel\",\n");
                exportData.append("  \"data\": [\n");
                
                if (userId != null) {
                    
                    UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, "1.0");
                    if (pattern != null) {
                        Map<String, Object> patternData = convertPatternToMap(pattern);
                        exportData.append("    {\n");
                        exportData.append("      \"用户ID\": ").append(patternData.get("userId")).append(",\n");
                        exportData.append("      \"用户名\": \"").append(patternData.get("username")).append("\",\n");
                        exportData.append("      \"平均日用电量\": ").append(patternData.get("averageDailyUsage")).append(",\n");
                        exportData.append("      \"高峰时段\": \"").append(patternData.get("peakHour")).append("\",\n");
                        exportData.append("      \"模式类型\": \"").append(patternData.get("patternType")).append("\",\n");
                        exportData.append("      \"预测月用电量\": ").append(patternData.get("predictedMonthlyUsage")).append(",\n");
                        exportData.append("      \"分析日期\": \"").append(patternData.get("analysisDate")).append("\"\n");
                        exportData.append("    }\n");
                    } else {
                        Map<String, Object> patternData = analyzeUserElectricityPattern(userId, Integer.parseInt(period));
                        exportData.append("    {\n");
                        exportData.append("      \"用户ID\": ").append(patternData.get("userId")).append(",\n");
                        exportData.append("      \"用户名\": \"").append(patternData.get("username")).append("\",\n");
                        exportData.append("      \"平均日用电量\": ").append(patternData.get("averageDailyUsage")).append(",\n");
                        exportData.append("      \"高峰时段\": \"").append(patternData.get("peakHour")).append("\",\n");
                        exportData.append("      \"模式类型\": \"").append(patternData.get("patternType")).append("\",\n");
                        exportData.append("      \"预测月用电量\": ").append(patternData.get("predictedMonthlyUsage")).append(",\n");
                        exportData.append("      \"分析日期\": \"").append(patternData.get("analysisDate")).append("\"\n");
                        exportData.append("    }\n");
                    }
                } else {
                    
                    exportData.append("    {\n");
                    exportData.append("      \"用户ID\": 1,\n");
                    exportData.append("      \"用户名\": \"示例用户\",\n");
                    exportData.append("      \"平均日用电量\": 25.6,\n");
                    exportData.append("      \"高峰时段\": \"19:00-22:00\",\n");
                    exportData.append("      \"模式类型\": \"住宅型\",\n");
                    exportData.append("      \"预测月用电量\": 780,\n");
                    exportData.append("      \"分析日期\": \"").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("\"\n");
                    exportData.append("    }\n");
                }
                
                exportData.append("  ]\n");
                exportData.append("}");
            }
            
            return ResponseEntity.ok(exportData.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("导出数据失败: " + e.getMessage());
        }
    }
    
    
    private Map<String, Object> convertPatternToMap(UserElectricityPattern pattern) {
        Map<String, Object> patternData = new HashMap<>();
        patternData.put("userId", pattern.getUserId());
        
        // 获取真实用户名
        String username = userService.getUserById(pattern.getUserId())
                .map(User::getName)
                .orElse("用户" + pattern.getUserId());
        patternData.put("username", username);
        
        try {
            if (pattern.getFeatureVector() != null) {
                JSONObject features = new JSONObject(pattern.getFeatureVector());
                
                // 计算日均用电量 (avgConsumption 是每15分钟的平均值)
                // 每日有 24 * 4 = 96 个 15分钟区间
                double avgConsumptionPerInterval = features.optDouble("avgConsumption", 0.0);
                double dailyUsage = avgConsumptionPerInterval * 96;
                patternData.put("averageDailyUsage", Math.round(dailyUsage * 10) / 10.0);
                
                // 预测月用电量
                patternData.put("predictedMonthlyUsage", Math.round(dailyUsage * 30));
                
                // 从 patternData 中寻找高峰时段
                if (pattern.getPatternData() != null) {
                     JSONObject hourlyData = new JSONObject(pattern.getPatternData());
                     String peakHour = "未知";
                     double maxMean = -1;
                     
                     // 遍历 00:00 到 23:00
                     for (int i = 0; i < 24; i++) {
                         String timeKey = String.format("%02d:00", i);
                         double mean = hourlyData.optDouble(timeKey + ".mean", 0.0);
                         if (mean > maxMean) {
                             maxMean = mean;
                             // 定义高峰时段为该小时及其后2小时 (e.g., 19:00 -> 19:00-22:00)
                             int endHour = (i + 3) % 24;
                             peakHour = String.format("%02d:00-%02d:00", i, endHour);
                         }
                     }
                     patternData.put("peakHour", peakHour);
                } else {
                    patternData.put("peakHour", "N/A");
                }
            } else {
                patternData.put("averageDailyUsage", 0);
                patternData.put("peakHour", "N/A");
                patternData.put("predictedMonthlyUsage", 0);
            }
        } catch (Exception e) {
            System.err.println("Error parsing pattern data for user " + pattern.getUserId() + ": " + e.getMessage());
            patternData.put("averageDailyUsage", 0);
            patternData.put("peakHour", "数据解析错误");
            patternData.put("predictedMonthlyUsage", 0);
        }
        
        patternData.put("patternType", pattern.getPatternType() != null ? pattern.getPatternType() : "未知");
        patternData.put("analysisDate", pattern.getAnalysisDate() != null ? 
                pattern.getAnalysisDate().format(DATE_TIME_FORMATTER) : 
                LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        return patternData;
    }
    
    
    private Map<String, Object> analyzeUserElectricityPattern(Long userId, int days) {
        
        List<com.powermanage.entity.ElectricityData> dataList = electricityDataService.getRecentElectricityDataByUserId(userId, days);
        
        // 获取真实用户名
        String username = userService.getUserById(userId)
                .map(User::getName)
                .orElse("用户" + userId);

        if (dataList.isEmpty()) {
            return Map.of(
                "userId", userId,
                "username", username,
                "averageDailyUsage", 0.0,
                "peakHour", "无数据",
                "patternType", "未知",
                "predictedMonthlyUsage", 0,
                "analysisDate", LocalDateTime.now().format(DATE_TIME_FORMATTER)
            );
        }
        
        // ... (rest of the logic)
        
        double totalUsage = dataList.stream()
            .mapToDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
            .sum();
        double averageDailyUsage = totalUsage / days;
        
        
        Map<String, Long> hourCount = dataList.stream()
            .collect(Collectors.groupingBy(
                data -> {
                    int hour = data.getRecordTime().getHour();
                    if (hour >= 0 && hour < 6) return "00:00-06:00";
                    else if (hour >= 6 && hour < 12) return "06:00-12:00";
                    else if (hour >= 12 && hour < 18) return "12:00-18:00";
                    else return "18:00-24:00";
                },
                Collectors.counting()
            ));
        
        String peakHour = hourCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("未知");
        
        
        String patternType = "住宅型"; 
        if (averageDailyUsage > 50) {
            patternType = "商业型";
        } else if (averageDailyUsage < 10) {
            patternType = "低耗型";
        }
        
        
        double predictedMonthlyUsage = averageDailyUsage * 30;
        
        return Map.of(
            "userId", userId,
            "username", username,
            "averageDailyUsage", Math.round(averageDailyUsage * 10) / 10.0,
            "peakHour", peakHour,
            "patternType", patternType,
            "predictedMonthlyUsage", Math.round(predictedMonthlyUsage),
            "analysisDate", LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
    }
}