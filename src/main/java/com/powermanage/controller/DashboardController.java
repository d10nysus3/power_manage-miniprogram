package com.powermanage.controller;

import com.powermanage.entity.ElectricityData;
import com.powermanage.service.ElectricityDataService;
import com.powermanage.service.UserService;
import com.powermanage.service.AnomalyDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RestController
public class DashboardController {

    
    @RestController
    @RequestMapping("/api/dashboard")
    static class SystemStatusController {
        
        @Autowired
        private UserService userService;
        
        @Autowired
        private AnomalyDetectionService anomalyDetectionService;
        
        @Autowired
        private ElectricityDataService electricityDataService;
        
        
        @GetMapping("/system-status")
        public Map<String, Object> getSystemStatus() {
            Map<String, Object> response = new HashMap<>();
            
            try {
                
                long totalUsers = userService.getTotalUserCount();
                response.put("totalUsers", totalUsers);
                
                
                LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
                LocalDateTime endOfToday = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
                long todayAnomalies = anomalyDetectionService.getAnomalyCountByTimeRange(startOfToday, endOfToday);
                response.put("todayAnomalies", todayAnomalies);
                
                
                long processedCount = anomalyDetectionService.getProcessedAnomalyCountByTimeRange(startOfToday, endOfToday);
                double processingRate = todayAnomalies > 0 ? (double) processedCount / todayAnomalies * 100 : 100.0;
                response.put("processingRate", Math.round(processingRate * 10) / 10.0);
                
                
                List<ElectricityData> todayElectricityData = electricityDataService.getElectricityDataByTimeRange(startOfToday, endOfToday);
                double todayElectricity = todayElectricityData.stream()
                    .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0.0)
                    .sum();
                response.put("todayElectricity", Math.round(todayElectricity * 10) / 10.0);
                
                
                LocalDateTime startOfYesterday = startOfToday.minusDays(1);
                LocalDateTime endOfYesterday = endOfToday.minusDays(1);
                
                
                
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);
                
                System.out.println("计算用户趋势，今天: " + today + ", 昨天: " + yesterday);
                
                
                long yesterdayUsers = 0;
                long todayUsers = 0;
                try {
                    yesterdayUsers = userService.getUserCountByDate(yesterday);
                    todayUsers = userService.getUserCountByDate(today);
                } catch (Exception e) {
                    System.err.println("获取用户数量时发生错误: " + e.getMessage());
                    e.printStackTrace();
                    
                    yesterdayUsers = 0;
                    todayUsers = 0;
                }
                double userTrend = yesterdayUsers > 0 ? ((double)(todayUsers - yesterdayUsers) / yesterdayUsers * 100) : 0.0;
                response.put("userTrend", Math.round(userTrend * 10) / 10.0);
                
                System.out.println("用户趋势计算完成，昨天用户数: " + yesterdayUsers + ", 今天用户数: " + todayUsers + ", 趋势: " + userTrend);
                
                
                long yesterdayAnomalies = anomalyDetectionService.getAnomalyCountByTimeRange(startOfYesterday, endOfYesterday);
                double anomalyTrend = yesterdayAnomalies > 0 ? ((double)(todayAnomalies - yesterdayAnomalies) / yesterdayAnomalies * 100) : 0.0;
                response.put("anomalyTrend", Math.round(anomalyTrend * 10) / 10.0);
                
                
                long yesterdayProcessed = anomalyDetectionService.getProcessedAnomalyCountByTimeRange(startOfYesterday, endOfYesterday);
                double yesterdayProcessingRate = yesterdayAnomalies > 0 ? (double) yesterdayProcessed / yesterdayAnomalies * 100 : 100.0;
                double rateTrend = processingRate - yesterdayProcessingRate;
                response.put("rateTrend", Math.round(rateTrend * 10) / 10.0);
                
                
                List<ElectricityData> yesterdayElectricityData = electricityDataService.getElectricityDataByTimeRange(startOfYesterday, endOfYesterday);
                double yesterdayElectricity = yesterdayElectricityData.stream()
                    .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0.0)
                    .sum();
                double electricityTrend = yesterdayElectricity > 0 ? ((todayElectricity - yesterdayElectricity) / yesterdayElectricity * 100) : 0.0;
                response.put("electricityTrend", Math.round(electricityTrend * 10) / 10.0);
                
                System.out.println("系统状态数据获取完成: " + response);
                
            } catch (Exception e) {
                System.err.println("获取系统状态时发生错误: " + e.getMessage());
                e.printStackTrace();
                response.put("error", "获取系统状态失败: " + e.getMessage());
            }
            
            return response;
        }
    }

    
    @RestController
    @RequestMapping("/api/anomalies")
    static class AnomalyController {
        private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        @Autowired
        private AnomalyDetectionService anomalyDetectionService;

        
        @GetMapping("/recent")
        public Map<String, Object> getRecentAnomalyRecords(@RequestParam(defaultValue = "5") int limit) {
            Map<String, Object> response = new HashMap<>();
            
            try {
                List<Map<String, Object>> records = anomalyDetectionService.getRecentAnomalies(limit);
                response.put("records", records);
                response.put("total", records.size());
                System.out.println("基于真实数据获取最近异常记录，记录数: " + records.size());
            } catch (Exception e) {
                System.err.println("获取最近异常记录失败: " + e.getMessage());
                e.printStackTrace();
                
                
                response.put("error", "获取异常记录失败: " + e.getMessage());
                response.put("records", new ArrayList<>());
                response.put("total", 0);
            }
            
            return response;
        }
        
        
        @GetMapping("/type-distribution")
        public List<Map<String, Object>> getAnomalyTypeDistribution() {
            try {
                List<Map<String, Object>> distribution = anomalyDetectionService.getAnomalyTypeDistribution();
                System.out.println("基于真实数据获取异常类型分布，类型数: " + distribution.size());
                return distribution;
            } catch (Exception e) {
                System.err.println("获取异常类型分布失败: " + e.getMessage());
                e.printStackTrace();
                
                
                return new ArrayList<>();
            }
        }
        
        
        @GetMapping("/debug")
        public Map<String, Object> debugAnomalyRecords() {
            Map<String, Object> response = new HashMap<>();
            try {
                List<Map<String, Object>> records = anomalyDetectionService.getRecentAnomalies(100);
                response.put("totalRecords", records.size());
                response.put("records", records);
                return response;
            } catch (Exception e) {
                System.err.println("调试异常记录失败: " + e.getMessage());
                e.printStackTrace();
                response.put("error", "调试异常记录失败: " + e.getMessage());
                return response;
            }
        }
    }

    
    @RestController
    @RequestMapping("/api/electricity-data")
    static class DashboardElectricityDataController {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
        
        @Autowired
        private ElectricityDataService electricityDataService;
        
        
        @GetMapping("/daily-stats")
        public List<Map<String, Object>> getDailyElectricityStats(@RequestParam(defaultValue = "7") int days) {
            List<Map<String, Object>> stats = new ArrayList<>();
            
            try {
                
                LocalDateTime startDateTime = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime endDateTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
                
                
                List<ElectricityData> electricityDataList = electricityDataService.getElectricityDataByTimeRange(
                    startDateTime, endDateTime);
                
                
                Map<LocalDate, Double> dailyUsageMap = new HashMap<>();
                for (ElectricityData data : electricityDataList) {
                    LocalDate date = data.getRecordTime().toLocalDate();
                    double consumption = data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0.0;
                    dailyUsageMap.merge(date, consumption, Double::sum);
                }
                
                
                for (int i = days - 1; i >= 0; i--) {
                    Map<String, Object> stat = new HashMap<>();
                    LocalDate date = LocalDate.now().minusDays(i);
                    stat.put("date", date.format(DATE_FORMATTER));
                    
                    
                    double dailyUsage = dailyUsageMap.getOrDefault(date, 0.0);
                    stat.put("value", Math.round(dailyUsage * 10) / 10.0); 
                    
                    stats.add(stat);
                }
                
                System.out.println("基于真实数据生成每日用电统计，数据条数: " + stats.size());
                
            } catch (Exception e) {
                System.err.println("获取真实用电数据失败: " + e.getMessage());
                e.printStackTrace();
                
                
                Map<String, Object> errorStat = new HashMap<>();
                errorStat.put("error", "获取用电数据失败: " + e.getMessage());
                stats.add(errorStat);
            }
            
            return stats;
        }
    }
}