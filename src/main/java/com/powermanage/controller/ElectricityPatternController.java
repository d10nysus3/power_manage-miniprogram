package com.powermanage.controller;

import com.powermanage.entity.UserElectricityPattern;
import com.powermanage.entity.User;
import com.powermanage.service.AnomalyDetectionService;
import com.powermanage.service.ElectricityDataService;
import com.powermanage.service.UserService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RestController
@RequestMapping("/api/electricity-patterns")
public class ElectricityPatternController {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ElectricityDataService electricityDataService;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @PostMapping("/build/{userId}")
    public ResponseEntity<UserElectricityPattern> buildUserElectricityPattern(@PathVariable Long userId) {
        try {
            UserElectricityPattern pattern = anomalyDetectionService.buildUserElectricityPattern(userId);
            return ResponseEntity.ok(pattern);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserElectricityPattern> getUserElectricityPattern(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1.0") String version) {
        try {
            UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, version);
            return pattern != null ? ResponseEntity.ok(pattern) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @PostMapping("/build/batch")
    public ResponseEntity<List<UserElectricityPattern>> batchBuildUserElectricityPatterns(@RequestBody List<Long> userIds) {
        try {
            List<UserElectricityPattern> patterns = anomalyDetectionService.batchBuildUserElectricityPatterns(userIds);
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    
    @GetMapping("/analysis")
    public ResponseEntity<List<Map<String, Object>>> getElectricityPatternAnalysis(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            List<Map<String, Object>> analysisResults = new ArrayList<>();
            
            if (userId != null) {
                processUserPattern(userId, days, analysisResults);
            } else {
                List<User> users = userService.getAllUsers();
                // 限制数量，防止请求过大
                users.stream().limit(50).forEach(user -> {
                    processUserPattern(user.getId(), days, analysisResults);
                });
            }
            
            return ResponseEntity.ok(analysisResults);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    private void processUserPattern(Long userId, int days, List<Map<String, Object>> results) {
        try {
            UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, "2.0");
            if (pattern == null) {
                pattern = anomalyDetectionService.getUserElectricityPattern(userId, "1.0");
            }

            if (pattern != null) {
                results.add(convertPatternToMap(pattern));
            } else {
                results.add(analyzeUserElectricityPattern(userId, days));
            }
        } catch (Exception e) {
            System.err.println("Error processing pattern for user " + userId + ": " + e.getMessage());
        }
    }

    private Map<String, Object> convertPatternToMap(UserElectricityPattern pattern) {
        Map<String, Object> patternData = new HashMap<>();
        patternData.put("userId", pattern.getUserId());
        
        String username = userService.getUserById(pattern.getUserId())
                .map(User::getName)
                .orElse("用户" + pattern.getUserId());
        patternData.put("username", username);
        
        try {
            if (pattern.getFeatureVector() != null) {
                JSONObject features = new JSONObject(pattern.getFeatureVector());
                double avgConsumptionPerInterval = features.optDouble("avgConsumption", 0.0);
                double dailyUsage = avgConsumptionPerInterval * 96;
                patternData.put("averageDailyUsage", Math.round(dailyUsage * 10) / 10.0);
                patternData.put("predictedMonthlyUsage", Math.round(dailyUsage * 30));
                
                if (pattern.getPatternData() != null) {
                     JSONObject hourlyData = new JSONObject(pattern.getPatternData());
                     String peakHour = "未知";
                     double maxMean = -1;
                     for (int i = 0; i < 24; i++) {
                         String timeKey = String.format("%02d:00", i);
                         double mean = hourlyData.optDouble(timeKey + ".mean", 0.0);
                         if (mean > maxMean) {
                             maxMean = mean;
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

    
    @PostMapping("/generate-report")
    public ResponseEntity<String> generateAnalysisReport(@RequestBody Map<String, Object> request) {
        try {
            Long userId = (Long) request.get("userId");
            String period = (String) request.get("period");
            
            
            
            return ResponseEntity.ok("分析报告生成成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("生成报告失败: " + e.getMessage());
        }
    }

    
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAnalysisData(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "30") String period,
            @RequestParam(required = false, defaultValue = "excel") String format) {
        try {
            List<Map<String, Object>> analysisResults = new ArrayList<>();
            int days = Integer.parseInt(period);
            
            if (userId != null) {
                processUserPattern(userId, days, analysisResults);
            } else {
                List<User> users = userService.getAllUsers();
                // 导出时可以多一些，比如100个
                users.stream().limit(100).forEach(user -> {
                    processUserPattern(user.getId(), days, analysisResults);
                });
            }
            
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("用电规律分析数据");
            
            Row headerRow = sheet.createRow(0);
            String[] headerTitles = {"用户ID", "用户名", "平均日用电量(kWh)", "用电高峰时段", "用电规律类型", "预测月用电量(kWh)", "分析日期"};
            for (int i = 0; i < headerTitles.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerTitles[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }
            
            for (int i = 0; i < analysisResults.size(); i++) {
                Map<String, Object> data = analysisResults.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(data.get("userId") != null ? data.get("userId").toString() : "");
                row.createCell(1).setCellValue(data.get("username") != null ? data.get("username").toString() : "");
                row.createCell(2).setCellValue(data.get("averageDailyUsage") != null ? Double.parseDouble(data.get("averageDailyUsage").toString()) : 0.0);
                row.createCell(3).setCellValue(data.get("peakHour") != null ? data.get("peakHour").toString() : "");
                row.createCell(4).setCellValue(data.get("patternType") != null ? data.get("patternType").toString() : "");
                row.createCell(5).setCellValue(data.get("predictedMonthlyUsage") != null ? Double.parseDouble(data.get("predictedMonthlyUsage").toString()) : 0.0);
                row.createCell(6).setCellValue(data.get("analysisDate") != null ? data.get("analysisDate").toString() : "");
            }
            
            for (int i = 0; i < headerTitles.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            byte[] bytes = outputStream.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "用电规律分析数据_" + (userId != null ? userId : "全部用户") + "_" + period + ".xlsx");
            headers.setContentLength(bytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
    
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllElectricityPatterns() {
        try {
            List<Map<String, Object>> patterns = new ArrayList<>();
            List<User> users = userService.getAllUsers();
            
            // 限制数量
            users.stream().limit(50).forEach(user -> {
                processUserPattern(user.getId(), 30, patterns);
            });
            
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
    
    private Map<String, Object> createPattern(Long userId, String username, double averageDailyUsage, 
                                             String peakHour, String patternType, int predictedMonthlyUsage) {
        Map<String, Object> pattern = new HashMap<>();
        pattern.put("id", userId); 
        pattern.put("userId", userId);
        pattern.put("username", username);
        pattern.put("averageDailyUsage", averageDailyUsage);
        pattern.put("peakHour", peakHour);
        pattern.put("patternType", patternType);
        pattern.put("predictedMonthlyUsage", predictedMonthlyUsage);
        pattern.put("analysisDate", "2024-01-25");
        return pattern;
    }
}
