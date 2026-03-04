package com.powermanage.controller;



import com.powermanage.service.ElectricityDataService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.stream.Collectors;



@RestController

@RequestMapping("/api/electricity")

public class ElectricityStatisticsController {



    @Autowired

    private ElectricityDataService electricityDataService;



    

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

            @RequestParam(required = false) String startDate,

            @RequestParam(required = false) String endDate,

            @RequestParam(required = false) String startTime,

            @RequestParam(required = false) String endTime,

            @RequestParam(required = false, defaultValue = "30") int days) {

        try {

            LocalDateTime start;

            LocalDateTime end;

            

            
            if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {

                start = java.time.LocalDate.parse(startTime).atStartOfDay();

                end = java.time.LocalDate.parse(endTime).atTime(23, 59, 59);

            }

            
            else if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {

                start = java.time.LocalDate.parse(startDate).atStartOfDay();

                end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);

            }

            
            else {

                start = LocalDateTime.now().minusDays(days);

                end = LocalDateTime.now();

            }

            

            List<com.powermanage.entity.ElectricityData> dataList;

            

            if (userId != null) {

                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);

            } else {

                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);

            }

            

            Map<String, Double> dailyStats = dataList.stream()

                .collect(java.util.stream.Collectors.groupingBy(

                    data -> data.getRecordTime().toLocalDate().toString(),

                    java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)

                ));

            

            
            java.util.LinkedHashMap<String, Double> sortedDailyStats = dailyStats.entrySet()

                .stream()

                .sorted(Map.Entry.comparingByKey())

                .collect(Collectors.toMap(

                    Map.Entry::getKey,

                    Map.Entry::getValue,

                    (oldValue, newValue) -> oldValue,

                    java.util.LinkedHashMap::new

                ));

            

            return ResponseEntity.ok(Map.of(

                "months", sortedDailyStats.keySet().toArray(),

                "usageData", sortedDailyStats.values().toArray()

            ));

        } catch (Exception e) {

            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));

        }

    }



    

    @GetMapping("/time-slot")
    public ResponseEntity<Map<String, Object>> getElectricityTimeSlot(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            LocalDateTime start;
            LocalDateTime end;

            if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
                start = java.time.LocalDate.parse(startTime).atStartOfDay();
                end = java.time.LocalDate.parse(endTime).atTime(23, 59, 59);
            } else if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
                start = java.time.LocalDate.parse(startDate).atStartOfDay();
                end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
            } else {
                start = LocalDateTime.now().minusDays(days);
                end = LocalDateTime.now();
            }

            List<com.powermanage.entity.ElectricityData> dataList;

            if (userId != null) {
                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            } else {
                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);
            }

            // 按小时分组 (0-23)
            Map<Integer, Double> hourlyStats = dataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    data -> data.getRecordTime().getHour(),
                    java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)
                ));

            // 构造 0-23 小时的完整数据
            List<Map<String, Object>> timeSlotData = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                Map<String, Object> data = new HashMap<>();
                data.put("hour", i);
                data.put("name", String.format("%02d:00", i));
                // 如果是查询多天的数据，这里显示的是总和，还是平均？
                // 通常时段分析看的是“习惯”，所以总和或平均都可以。
                // 如果为了反映“强度”，平均值可能更好；如果反映“总量”，总和更好。
                // 原代码是 summingDouble (总和)。这里保持总和，或者可以在前端处理。
                // 为了避免歧义，这里返回该时段的总用电量。
                data.put("value", hourlyStats.getOrDefault(i, 0.0));
                timeSlotData.add(data);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("timeSlotData", timeSlotData);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }



    

    @GetMapping("/prediction")

    public ResponseEntity<Map<String, Object>> getElectricityPrediction(

            @RequestParam(required = false) Long userId,

            @RequestParam(required = false) String startDate,

            @RequestParam(required = false) String endDate,

            @RequestParam(required = false) String startTime,

            @RequestParam(required = false) String endTime,

            @RequestParam(required = false, defaultValue = "30") int days) {

        try {

            LocalDateTime start;

            LocalDateTime end;

            

            
            if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {

                start = java.time.LocalDate.parse(startTime).atStartOfDay();

                end = java.time.LocalDate.parse(endTime).atTime(23, 59, 59);

            }

            
            else if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {

                start = java.time.LocalDate.parse(startDate).atStartOfDay();

                end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);

            }

            
            else {

                start = LocalDateTime.now().minusDays(days);

                end = LocalDateTime.now();

            }

            

            List<com.powermanage.entity.ElectricityData> dataList;

            

            if (userId != null) {

                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);

            } else {

                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);

            }

            

            

            Map<String, Double> dailyStatsUnsorted = dataList.stream()

                .collect(java.util.stream.Collectors.groupingBy(

                    data -> data.getRecordTime().toLocalDate().toString(),

                    java.util.stream.Collectors.summingDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)

                ));

            

            

            java.util.LinkedHashMap<String, Double> dailyStats = dailyStatsUnsorted.entrySet()

                .stream()

                .sorted(Map.Entry.comparingByKey())

                .collect(Collectors.toMap(

                    Map.Entry::getKey,

                    Map.Entry::getValue,

                    (oldValue, newValue) -> oldValue,

                    java.util.LinkedHashMap::new

                ));

            

            

            List<String> sortedMonths = new ArrayList<>(dailyStats.keySet());

            List<Double> historicalData = new ArrayList<>(dailyStats.values());

            

            

            List<Double> predictedData = generatePrediction(historicalData, 30);

            

            

            if (!sortedMonths.isEmpty()) {

                java.time.LocalDate lastDate = java.time.LocalDate.parse(sortedMonths.get(sortedMonths.size() - 1));

                for (int i = 1; i <= predictedData.size(); i++) {

                    sortedMonths.add(lastDate.plusDays(i).toString());

                }

            }

            

            

            

            List<Double> historicalForChart = new ArrayList<>(historicalData);

            for (int i = 0; i < predictedData.size(); i++) {

                historicalForChart.add(null);

            }

            

            

            List<Double> predictedForChart = new ArrayList<>();

            for (int i = 0; i < historicalData.size(); i++) {

                predictedForChart.add(null);

            }

            

            if (!historicalData.isEmpty()) {

                predictedForChart.set(predictedForChart.size() - 1, historicalData.get(historicalData.size() - 1));

            }

            

            predictedForChart.addAll(predictedData);

            

            return ResponseEntity.ok(Map.of(

                "months", sortedMonths.toArray(),

                "historicalData", historicalForChart.toArray(),

                "predictedData", predictedForChart.toArray()

            ));

        } catch (Exception e) {

            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));

        }

    }



    

    @GetMapping("/realtime-stats")

    public ResponseEntity<Map<String, Object>> getRealtimeMonitoringData() {

        try {

            

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            List<com.powermanage.entity.ElectricityData> recentData = electricityDataService.getElectricityDataByTimeRange(oneHourAgo, LocalDateTime.now());

            

            

            double currentUsage = recentData.isEmpty() ? 0.0 : recentData.get(recentData.size() - 1).getElectricityConsumption();

            

            

            String status = determineSystemStatus(recentData);

            

            return ResponseEntity.ok(Map.of(

                "currentUsage", currentUsage,

                "status", status,

                "timestamp", LocalDateTime.now().toString()

            ));

        } catch (Exception e) {

            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));

        }

    }



    

    private List<Double> generatePrediction(List<Double> historicalData, int predictionDays) {
        List<Double> predictions = new ArrayList<>();
        
        if (historicalData.isEmpty()) {
            for (int i = 0; i < predictionDays; i++) {
                predictions.add(0.0); 
            }
            return predictions;
        }
        
        if (historicalData.size() < 3) {
            double avgValue = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            for (int i = 0; i < predictionDays; i++) {
                predictions.add(avgValue);
            }
            return predictions;
        }
        
        int windowSize = Math.min(7, historicalData.size());

        

        

        double recentAvg = 0.0;

        for (int i = historicalData.size() - windowSize; i < historicalData.size(); i++) {

            recentAvg += historicalData.get(i);

        }

        recentAvg /= windowSize;

        

        

        double trend = 0.0;

        if (historicalData.size() >= 6) {

            double firstHalfAvg = 0.0, secondHalfAvg = 0.0;

            int halfSize = historicalData.size() / 2;

            

            for (int i = 0; i < halfSize; i++) {

                firstHalfAvg += historicalData.get(i);

            }

            for (int i = halfSize; i < historicalData.size(); i++) {

                secondHalfAvg += historicalData.get(i);

            }

            

            firstHalfAvg /= halfSize;

            secondHalfAvg /= (historicalData.size() - halfSize);

            trend = (secondHalfAvg - firstHalfAvg) / halfSize;

        }

        

        

        trend = Math.max(-2.0, Math.min(2.0, trend));

        

        

        for (int i = 1; i <= predictionDays; i++) {

            

            double basePrediction = recentAvg + trend * i;

            

            

            double fluctuationFactor = 0.98 + (Math.random() * 0.04); 

            double predictedValue = basePrediction * fluctuationFactor;

            

            

            predictedValue = Math.max(1.0, predictedValue);

            

            

            predictions.add(Math.round(predictedValue * 100.0) / 100.0);

        }

        

        return predictions;

    }



    

    private String determineSystemStatus(List<com.powermanage.entity.ElectricityData> recentData) {

        if (recentData.isEmpty()) {

            return "normal";

        }

        

        

        double avgUsage = recentData.stream()

            .mapToDouble(com.powermanage.entity.ElectricityData::getElectricityConsumption)

            .average()

            .orElse(0.0);

        

        

        if (avgUsage > 100) {

            return "warning";

        } else if (avgUsage > 200) {

            return "critical";

        } else {

            return "normal";

        }

    }

}

