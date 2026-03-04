package com.powermanage.controller;

import com.powermanage.service.ElectricityDataService;
import com.powermanage.service.ElectricityPatternAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/electricity-statistics")
public class ElectricityDataStatisticsController {

    @Autowired
    private ElectricityDataService electricityDataService;
    
    @Autowired
    private ElectricityPatternAnalysisService electricityPatternAnalysisService;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getElectricityStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            List<com.powermanage.entity.ElectricityData> dataList;
            
            if (userId != null) {
                dataList = electricityDataService.getRecentElectricityDataByUserId(userId, days);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(startTime, LocalDateTime.now());
            }
            
            
            Map<String, Double> monthlyStats = dataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    data -> data.getRecordTime().toLocalDate().toString().substring(0, 7),
                    java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
                ));
            
            return ResponseEntity.ok(Map.of(
                "months", monthlyStats.keySet().toArray(),
                "usageData", monthlyStats.values().toArray()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

@GetMapping("/trend")
public ResponseEntity<Map<String, Object>> getElectricityTrend(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false, defaultValue = "30") int days,
        
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
    try {
        List<com.powermanage.entity.ElectricityData> dataList;
        
        
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            
            LocalDateTime start = LocalDateTime.parse(startTime + " 00:00:00", DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime + " 23:59:59", DATE_TIME_FORMATTER);
            if (userId != null) {
                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);
            }
        } else {
            
            LocalDateTime start = LocalDateTime.now().minusDays(days);
            if (userId != null) {
                dataList = electricityDataService.getRecentElectricityDataByUserId(userId, days);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(start, LocalDateTime.now());
            }
        }
        
        Map<String, Double> dailyStats = dataList.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                data -> data.getRecordTime().toLocalDate().toString(),
                java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
            ));
        
        return ResponseEntity.ok(Map.of(
            "months", dailyStats.keySet().toArray(),
            "usageData", dailyStats.values().toArray()
        ));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
@GetMapping("/time-slot")
public ResponseEntity<Map<String, Object>> getElectricityTimeSlot(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false, defaultValue = "30") int days,
        
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
    try {
        List<com.powermanage.entity.ElectricityData> dataList;
        
        
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime + " 00:00:00", DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime + " 23:59:59", DATE_TIME_FORMATTER);
            if (userId != null) {
                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);
            }
        } else {
            
            LocalDateTime start = LocalDateTime.now().minusDays(days);
            if (userId != null) {
                dataList = electricityDataService.getRecentElectricityDataByUserId(userId, days);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(start, LocalDateTime.now());
            }
        }
        
        Map<String, Double> timeSlotStats = dataList.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                data -> {
                    int hour = data.getRecordTime().getHour();
                    if (hour >= 0 && hour < 6) return "00:00-06:00";
                    else if (hour >= 6 && hour < 12) return "06:00-12:00";
                    else if (hour >= 12 && hour < 18) return "12:00-18:00";
                    else return "18:00-24:00";
                },
                java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
            ));
        
        List<Map<String, Object>> timeSlotData = timeSlotStats.entrySet().stream()
            .map(entry -> {
                Map<String, Object> data = new HashMap<>();
                data.put("name", entry.getKey());
                data.put("value", entry.getValue());
                return data;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("timeSlotData", timeSlotData);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

    @GetMapping("/time-range")
    public ResponseEntity<List<com.powermanage.entity.ElectricityData>> getElectricityDataByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Long userId) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
            
            List<com.powermanage.entity.ElectricityData> dataList;
            if (userId != null) {
                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);
            }
            
            return ResponseEntity.ok(dataList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
@GetMapping("/prediction")
public ResponseEntity<Map<String, Object>> getElectricityPrediction(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false, defaultValue = "30") int days,
        
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
    try {
        if (userId != null) {
            Map<String, Object> prediction = electricityPatternAnalysisService.predictElectricityUsage(userId, days);
            
            Map<String, Object> result = new HashMap<>();
            
            
            List<com.powermanage.entity.ElectricityData> dataList;
            if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(startTime + " 00:00:00", DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endTime + " 23:59:59", DATE_TIME_FORMATTER);
                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            } else {
                LocalDateTime start = LocalDateTime.now().minusDays(days);
                dataList = electricityDataService.getRecentElectricityDataByUserId(userId, days);
            }
            
            Map<String, Double> dailyStats = dataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    data -> data.getRecordTime().toLocalDate().toString(),
                    java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
                ));
            
            List<Double> historicalData = dailyStats.values().stream().collect(Collectors.toList());
            
            Double predictedDailyUsage = (Double) prediction.get("predictedDailyUsage");
            Double predictedTotalUsage = (Double) prediction.get("predictedTotalUsage");
            List<Double> predictedData = new ArrayList<>();
            
            if (predictedDailyUsage != null) {
                Random random = new Random();
                double variance = 0.0;
                if (!historicalData.isEmpty()) {
                    double mean = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    variance = historicalData.stream()
                        .mapToDouble(x -> Math.pow(x - mean, 2))
                        .average()
                        .orElse(0.0);
                }
                double stdDev = Math.sqrt(variance);
                double maxVariation = Math.min(stdDev * 0.5, predictedDailyUsage * 0.3);
                
                Map<Integer, Double> weeklyPattern = new HashMap<>();
                if (prediction.containsKey("weeklyPattern")) {
                    try {
                        weeklyPattern = (Map<Integer, Double>) prediction.get("weeklyPattern");
                    } catch (Exception e) {
                        
                    }
                }
                
                double weeklyPatternAvg = weeklyPattern.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(1.0);
                
                for (int i = 0; i < 30; i++) {
                    
                    
                    
                    int dayOfWeek = (LocalDateTime.now().plusDays(i).getDayOfWeek().getValue() % 7);
                    double weeklyFactor = 1.0;
                    if (weeklyPattern.containsKey(dayOfWeek) && weeklyPatternAvg > 0) {
                        weeklyFactor = weeklyPattern.get(dayOfWeek) / weeklyPatternAvg;
                        weeklyFactor = Math.max(0.7, Math.min(weeklyFactor, 1.3));
                    } else {
                        weeklyFactor = (dayOfWeek == 0 || dayOfWeek == 6) ? 0.85 : 1.05;
                    }
                    
                    
                    double dailyPrediction = predictedDailyUsage * weeklyFactor; 
                    dailyPrediction = Math.max(predictedDailyUsage * 0.5, Math.min(dailyPrediction, predictedDailyUsage * 1.5));
                    predictedData.add(Math.round(dailyPrediction * 100.0) / 100.0);
                }
            }
            
            result.put("months", dailyStats.keySet().toArray());
            result.put("historicalData", historicalData.toArray());
            result.put("predictedData", predictedData.toArray());
            result.put("predictedMonthlyUsage", predictedTotalUsage != null ? predictedTotalUsage : (predictedDailyUsage != null ? predictedDailyUsage * 30 : 0));
            
            return ResponseEntity.ok(result);
        } else {
            
            LocalDateTime dataStartTime;
            List<com.powermanage.entity.ElectricityData> dataList;

            if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                dataStartTime = LocalDateTime.parse(startTime + " 00:00:00", DATE_TIME_FORMATTER);
                LocalDateTime dataEndTime = LocalDateTime.parse(endTime + " 23:59:59", DATE_TIME_FORMATTER);
                dataList = electricityDataService.getElectricityDataByTimeRange(dataStartTime, dataEndTime);
            } else {
                dataStartTime = LocalDateTime.now().minusDays(days);
                dataList = electricityDataService.getElectricityDataByTimeRange(dataStartTime, LocalDateTime.now());
            }

            Map<String, Double> dailyStats = dataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    data -> data.getRecordTime().toLocalDate().toString(),
                    java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
                ));
            
            List<Double> historicalData = dailyStats.values().stream().collect(Collectors.toList());
            List<Double> predictedData = generatePrediction(historicalData, 6);
            
            return ResponseEntity.ok(Map.of(
                "months", dailyStats.keySet().toArray(),
                "historicalData", historicalData.toArray(),
                "predictedData", predictedData.toArray()
            ));
        }
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
    private List<Double> generatePrediction(List<Double> historicalData, int predictionDays) {
        
        if (historicalData.size() < 5) { 
            double lastValue = historicalData.isEmpty() ? 0 : historicalData.get(historicalData.size() - 1);
            return java.util.stream.Stream.generate(() -> lastValue).limit(predictionDays).collect(Collectors.toList());
        }
        
        
        List<Double> processedData = handleAnomalies(historicalData);
        
        
        List<Double> predictedData = predictUsingMovingAverage(processedData, predictionDays);
        
        return predictedData;
    }
    
    
    private List<Double> handleAnomalies(List<Double> data) {
        
        if (data.size() < 5) {
            return new ArrayList<>(data);
        }
        
        
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = data.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        
        double threshold = 3.0;
        
        
        List<Double> cleanedData = new ArrayList<>();
        
        
        for (double value : data) {
            
            double zScore = 0;
            if (stdDev != 0) {
                zScore = Math.abs(value - mean) / stdDev;
            }
            
            
            if (zScore > threshold) {
                
                cleanedData.add(mean);
            } else {
                
                cleanedData.add(value);
            }
        }
        
        return cleanedData;
    }
    
    
    private List<Double> predictUsingMovingAverage(List<Double> data, int predictionDays) {
        
        int windowSize = 7;
        
        
        if (data.size() < windowSize) {
            double lastValue = data.isEmpty() ? 0 : data.get(data.size() - 1);
            return java.util.stream.Stream.generate(() -> lastValue).limit(predictionDays).collect(Collectors.toList());
        }
        
        List<Double> predictions = new ArrayList<>();
        
        List<Double> tempData = new ArrayList<>(data);
        
        
        for (int i = 0; i < predictionDays; i++) {
            
            int startIndex = tempData.size() - windowSize;
            List<Double> recentData = tempData.subList(startIndex, tempData.size());
            
            
            double nextPrediction = recentData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            
            predictions.add(nextPrediction);
            
            
            tempData.add(nextPrediction);
        }
        
        return predictions;
    }
    
    @GetMapping("/detailed-stats")
    public ResponseEntity<Map<String, Object>> getDetailedStats(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "7") String period) {
        try {
            
            int days = 7;
            if (period.endsWith("d")) {
                days = Integer.parseInt(period.substring(0, period.length() - 1));
            } else if (period.endsWith("m")) {
                days = Integer.parseInt(period.substring(0, period.length() - 1)) * 30;
            }
            
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            List<com.powermanage.entity.ElectricityData> dataList = 
                electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, startTime, LocalDateTime.now());
            
            if (dataList.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "totalUsage", 0,
                    "avgUsage", 0,
                    "maxUsage", 0,
                    "minUsage", 0,
                    "peakHour", "12:00",
                    "offPeakHour", "03:00",
                    "dataPoints", 0
                ));
            }
            
            
            double totalUsage = dataList.stream()
                .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                .sum();
            
            double avgUsage = totalUsage / dataList.size();
            
            double maxUsage = dataList.stream()
                .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                .max().orElse(0);
            
            double minUsage = dataList.stream()
                .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                .min().orElse(0);
            
            
            Map<Integer, Double> hourlyUsage = dataList.stream()
                .collect(Collectors.groupingBy(
                    data -> data.getRecordTime().getHour(),
                    Collectors.summingDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                ));
            
            String peakHour = hourlyUsage.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> String.format("%02d:00", entry.getKey()))
                .orElse("12:00");
            
            String offPeakHour = hourlyUsage.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(entry -> String.format("%02d:00", entry.getKey()))
                .orElse("03:00");
            
            return ResponseEntity.ok(Map.of(
                "totalUsage", totalUsage,
                "avgUsage", avgUsage,
                "maxUsage", maxUsage,
                "minUsage", minUsage,
                "peakHour", peakHour,
                "offPeakHour", offPeakHour,
                "dataPoints", dataList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> getAlerts(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "7") String period) {
        try {
            
            int days = 7;
            if (period.endsWith("d")) {
                days = Integer.parseInt(period.substring(0, period.length() - 1));
            } else if (period.endsWith("m")) {
                days = Integer.parseInt(period.substring(0, period.length() - 1)) * 30;
            }
            
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            List<com.powermanage.entity.ElectricityData> dataList = 
                electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, startTime, LocalDateTime.now());
            
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            if (!dataList.isEmpty()) {
                
                double avgUsage = dataList.stream()
                    .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                    .average().orElse(0);
                
                
                dataList.forEach(data -> {
                    if (data.getElectricityConsumption() != null && data.getElectricityConsumption() > avgUsage * 1.5) {
                        Map<String, Object> alert = new HashMap<>();
                        alert.put("type", "高用电量");
                        alert.put("level", "warning");
                        alert.put("time", data.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        alert.put("value", data.getElectricityConsumption());
                        alert.put("description", "用电量超过平均值50%");
                        alerts.add(alert);
                    }
                });
                
                
                if (alerts.isEmpty()) {
                    Map<String, Object> noAlert = new HashMap<>();
                    noAlert.put("type", "无异常");
                    noAlert.put("level", "info");
                    noAlert.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    noAlert.put("value", 0);
                    noAlert.put("description", "用电模式正常");
                    alerts.add(noAlert);
                }
            } else {
                Map<String, Object> noDataAlert = new HashMap<>();
                noDataAlert.put("type", "数据缺失");
                noDataAlert.put("level", "warning");
                noDataAlert.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                noDataAlert.put("value", 0);
                noDataAlert.put("description", "指定时间段内无用电数据");
                alerts.add(noDataAlert);
            }
            
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of(
                "error", e.getMessage(),
                "type", "系统错误",
                "level", "error"
            )));
        }
    }
    
    @GetMapping("/suggestions")
    public ResponseEntity<List<Map<String, Object>>> getSuggestions(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "7") String period) {
        try {
            
            int days = 7;
            if (period.endsWith("d")) {
                days = Integer.parseInt(period.substring(0, period.length() - 1));
            } else if (period.endsWith("m")) {
                days = Integer.parseInt(period.substring(0, period.length() - 1)) * 30;
            }
            
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            List<com.powermanage.entity.ElectricityData> dataList = 
                electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, startTime, LocalDateTime.now());
            
            List<Map<String, Object>> suggestions = new ArrayList<>();
            
            if (!dataList.isEmpty()) {
                
                double totalUsage = dataList.stream()
                    .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                    .sum();
                
                double avgUsage = totalUsage / dataList.size();
                
                
                Map<Integer, Double> hourlyUsage = dataList.stream()
                    .collect(Collectors.groupingBy(
                        data -> data.getRecordTime().getHour(),
                        Collectors.summingDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                    ));
                
                
                if (avgUsage > 50) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("type", "节能建议");
                    suggestion.put("priority", "high");
                    suggestion.put("title", "优化高耗能设备使用");
                    suggestion.put("description", "您的平均用电量较高，建议检查高耗能设备的使用情况，合理安排使用时间");
                    suggestions.add(suggestion);
                }
                
                
                double maxHourly = hourlyUsage.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
                double minHourly = hourlyUsage.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
                
                if (maxHourly > 0 && minHourly > 0 && maxHourly / minHourly > 3) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("type", "错峰用电");
                    suggestion.put("priority", "medium");
                    suggestion.put("title", "实施错峰用电");
                    suggestion.put("description", "您的用电峰谷差异较大，建议将部分用电活动调整至低谷时段，可降低用电成本");
                    suggestions.add(suggestion);
                }
                
                
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("type", "维护建议");
                suggestion.put("priority", "low");
                suggestion.put("title", "定期设备维护");
                suggestion.put("description", "定期检查和维护用电设备，保持设备良好运行状态，可提高能源利用效率");
                suggestions.add(suggestion);
            } else {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("type", "数据建议");
                suggestion.put("priority", "high");
                suggestion.put("title", "完善数据采集");
                suggestion.put("description", "当前时间段内缺少用电数据，建议检查数据采集设备是否正常运行");
                suggestions.add(suggestion);
            }
            
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of(
                "error", e.getMessage(),
                "type", "系统错误",
                "priority", "high"
            )));
        }
    }
}
