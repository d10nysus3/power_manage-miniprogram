package com.powermanage.controller;

import com.powermanage.entity.UserElectricityPattern;
import com.powermanage.service.ElectricityPatternAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pattern-analysis")
public class ElectricityPatternAnalysisController {

    @Autowired
    private ElectricityPatternAnalysisService electricityPatternAnalysisService;

    @PostMapping("/generate/{userId}")
    public ResponseEntity<?> generateUserPattern(@PathVariable Long userId) {
        try {
            UserElectricityPattern pattern = electricityPatternAnalysisService.generateAndSaveUserPattern(userId);
            return ResponseEntity.ok(pattern);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "生成用户画像失败: " + e.getMessage()));
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserPattern(@PathVariable Long userId) {
        try {
            UserElectricityPattern pattern = electricityPatternAnalysisService.getUserPattern(userId);
            if (pattern != null) {
                return ResponseEntity.ok(pattern);
            } else {
                return ResponseEntity.status(404).body(Map.of("message", "未找到该用户的画像信息"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "获取用户画像失败: " + e.getMessage()));
        }
    }

    @GetMapping("/prediction")
    public ResponseEntity<Map<String, Object>> predictElectricityUsage(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "30") int period) {
        try {
            Map<String, Object> prediction = electricityPatternAnalysisService.predictElectricityUsage(userId, period);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "预测过程中出现错误: " + e.getMessage()));
        }
    }

    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> analyzePatternTrend(
            @RequestParam Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
            Map<String, Object> trend = electricityPatternAnalysisService.analyzePatternTrend(userId, start, end);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "趋势分析过程中出现错误: " + e.getMessage()));
        }
    }
}