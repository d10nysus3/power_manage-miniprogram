package com.powermanage.controller;

import com.powermanage.entity.ElectricityData;
import com.powermanage.service.ElectricityDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/electricity")
public class RealtimeMonitoringController {

    @Autowired
    private ElectricityDataService electricityDataService;

    
    @GetMapping("/realtime-monitoring")
    public ResponseEntity<Map<String, Object>> getRealtimeMonitoringData() {
        try {
            
            List<ElectricityData> recentData = electricityDataService.getRecentElectricityData(10);
            
            
            double currentUsage = 0.0;
            String status = "normal";
            String timestamp = LocalDateTime.now().toString();
            
            if (!recentData.isEmpty()) {
                ElectricityData latestData = recentData.get(0);
                currentUsage = latestData.getPower() != null ? latestData.getPower() : 0.0;
                
                
                if (currentUsage > 5.0) {
                    status = "warning";
                } else if (currentUsage > 8.0) {
                    status = "critical";
                }
                
                timestamp = latestData.getRecordTime().toString();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("currentUsage", currentUsage);
            result.put("status", status);
            result.put("timestamp", timestamp);
            result.put("recentData", recentData);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("currentUsage", 0.0);
            errorResult.put("status", "error");
            errorResult.put("timestamp", LocalDateTime.now().toString());
            errorResult.put("error", e.getMessage());
            
            return ResponseEntity.ok(errorResult);
        }
    }

    
    @GetMapping("/realtime-trend")
    public ResponseEntity<Map<String, Object>> getRealtimeTrendData(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);
            
            List<ElectricityData> trendData = electricityDataService.getElectricityDataByTimeRange(startTime, endTime);
            
            Map<String, Object> result = new HashMap<>();
            result.put("trendData", trendData);
            result.put("startTime", startTime.toString());
            result.put("endTime", endTime.toString());
            result.put("totalRecords", trendData.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("trendData", List.of());
            
            return ResponseEntity.ok(errorResult);
        }
    }

    
    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<ElectricityData> recentData = electricityDataService.getElectricityDataByTimeRange(oneHourAgo, LocalDateTime.now());
            
            
            double avgUsage = recentData.stream()
                    .mapToDouble(data -> data.getPower() != null ? data.getPower() : 0.0)
                    .average()
                    .orElse(0.0);
            
            
            double maxUsage = recentData.stream()
                    .mapToDouble(data -> data.getPower() != null ? data.getPower() : 0.0)
                    .max()
                    .orElse(0.0);
            
            status.put("avgUsage", avgUsage);
            status.put("maxUsage", maxUsage);
            status.put("totalRecords", recentData.size());
            status.put("systemStatus", "normal");
            status.put("lastUpdate", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("systemStatus", "error");
            errorStatus.put("error", e.getMessage());
            errorStatus.put("lastUpdate", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(errorStatus);
        }
    }
}