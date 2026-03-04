package com.powermanage.service.impl;

import com.powermanage.entity.ElectricityData;
import com.powermanage.entity.AnomalyDetectionRecord;
import com.powermanage.repository.ElectricityDataRepository;
import com.powermanage.service.ElectricityDataService;
import com.powermanage.service.AnomalyDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class ElectricityDataServiceImpl implements ElectricityDataService {

    @Autowired
    private ElectricityDataRepository electricityDataRepository;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    
    private static final Map<String, byte[]> reportStorage = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ElectricityData saveElectricityData(ElectricityData electricityData) {
        ElectricityData savedData = electricityDataRepository.save(electricityData);
        
        // 触发异常检测
        try {
            anomalyDetectionService.detectAnomaly(savedData);
        } catch (Exception e) {
            System.err.println("异常检测触发失败: " + e.getMessage());
            // 不中断正常的数据保存流程
        }
        
        return savedData;
    }

    @Override
    public List<ElectricityData> getElectricityDataByUserId(Long userId) {
        return electricityDataRepository.findByUser_Id(userId);
    }

    @Override
    public List<ElectricityData> getElectricityDataByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return electricityDataRepository.findByRecordTimeBetween(startTime, endTime);
    }

    @Override
    public List<ElectricityData> getElectricityDataByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return electricityDataRepository.findByUser_IdAndRecordTimeBetween(userId, startTime, endTime);
    }

    @Override
    public List<ElectricityData> getRecentElectricityDataByUserId(Long userId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        return electricityDataRepository.findByUser_IdAndRecordTimeAfter(userId, startTime);
    }

    @Override
    public List<ElectricityData> batchSaveElectricityData(List<ElectricityData> electricityDataList) {
        List<ElectricityData> savedList = electricityDataRepository.saveAll(electricityDataList);
        
        // 触发批量异常检测
        try {
            anomalyDetectionService.batchDetectAnomalies(savedList);
        } catch (Exception e) {
            System.err.println("批量异常检测触发失败: " + e.getMessage());
        }
        
        return savedList;
    }

    @Override
    public List<ElectricityData> getAllElectricityData() {
        return electricityDataRepository.findAll();
    }

    @Override
    public ElectricityData getElectricityDataById(Long id) {
        Optional<ElectricityData> optional = electricityDataRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public void deleteElectricityData(Long id) {
        electricityDataRepository.deleteById(id);
    }

    @Override
    public ElectricityData updateElectricityData(ElectricityData electricityData) {
        return electricityDataRepository.save(electricityData);
    }

    @Override
    public List<ElectricityData> getRecentElectricityData(int count) {
        return electricityDataRepository.findTop10ByOrderByRecordTimeDesc()
                .stream()
                .limit(count)
                .collect(Collectors.toList());
    }
    
    @Override
    public double getTotalElectricityConsumptionByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<ElectricityData> dataList = electricityDataRepository.findByRecordTimeBetween(startOfDay, endOfDay);
        
        
        return dataList.stream()
                .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                .sum();
    }

    @Override
    public byte[] generateUserReport(Long userId) {
        try {
            List<ElectricityData> userData = getElectricityDataByUserId(userId);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            String reportContent = "用户用电数据报告\n";
            reportContent += "用户ID: " + userId + "\n";
            reportContent += "生成时间: " + LocalDateTime.now() + "\n\n";
            
            reportContent += "数据统计:\n";
            reportContent += "总记录数: " + userData.size() + "\n";
            
            double totalConsumption = userData.stream()
                .mapToDouble(data -> data.getElectricityConsumption() != null ? data.getElectricityConsumption() : 0)
                .sum();
            reportContent += "总用电量: " + totalConsumption + " kWh\n";
            
            if (!userData.isEmpty()) {
                double avgConsumption = totalConsumption / userData.size();
                reportContent += "平均用电量: " + avgConsumption + " kWh\n";
                
                final StringBuilder finalReportContent = new StringBuilder(reportContent);
                finalReportContent.append("\n最近10条记录:\n");
                userData.stream()
                    .sorted((a, b) -> b.getRecordTime().compareTo(a.getRecordTime()))
                    .limit(10)
                    .forEach(data -> {
                        finalReportContent.append("时间: ").append(data.getRecordTime())
                                       .append(", 用电量: ").append(data.getElectricityConsumption()).append(" kWh\n");
                    });
                reportContent = finalReportContent.toString();
            }
            
            outputStream.write(reportContent.getBytes());
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成用户报告失败", e);
        }
    }

    @Override
    public String generateReport(Long userId, LocalDateTime startTime, LocalDateTime endTime, String reportType) {
        String reportId = UUID.randomUUID().toString();
        
        List<ElectricityData> data;
        if (userId != null && startTime != null && endTime != null) {
            data = getElectricityDataByUserIdAndTimeRange(userId, startTime, endTime);
        } else if (userId != null) {
            data = getElectricityDataByUserId(userId);
        } else if (startTime != null && endTime != null) {
            data = getElectricityDataByTimeRange(startTime, endTime);
        } else {
            data = getAllElectricityData();
        }
        
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            StringBuilder reportContent = new StringBuilder();
            reportContent.append("========================================\n");
            reportContent.append("用电规律分析报告\n");
            reportContent.append("========================================\n");
            
            if (userId != null) {
                reportContent.append("报告对象: 用户 ").append(userId).append("\n");
            } else {
                reportContent.append("报告对象: 全部用户\n");
            }
            
            if (startTime != null && endTime != null) {
                reportContent.append("报告周期: ").append(startTime.toLocalDate()).append(" 至 ").append(endTime.toLocalDate())
                             .append(" (").append(java.time.temporal.ChronoUnit.DAYS.between(startTime, endTime)).append(" 天)\n");
            }
            
            reportContent.append("生成日期: ").append(LocalDate.now()).append("\n\n");
            
            if (!data.isEmpty()) {
                
                Map<LocalDate, Double> dailyConsumption = data.stream()
                    .filter(d -> d.getElectricityConsumption() != null)
                    .collect(Collectors.groupingBy(
                        d -> d.getRecordTime().toLocalDate(),
                        Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                    ));
                
                double totalConsumption = dailyConsumption.values().stream().mapToDouble(Double::doubleValue).sum();
                double averageDailyUsage = dailyConsumption.size() > 0 ? totalConsumption / dailyConsumption.size() : 0;
                double predictedMonthlyUsage = averageDailyUsage * 30;
                
                
                double estimatedCost = totalConsumption * 0.6;
                
                reportContent.append("--- 核心指标 ---\n");
                reportContent.append(String.format("平均日用电量: %.1f kWh\n", averageDailyUsage));
                reportContent.append(String.format("预测月用电量: %.0f kWh\n", predictedMonthlyUsage));
                reportContent.append(String.format("预估总电费: ¥%.2f (按0.6元/度估算)\n", estimatedCost));
                
                
                if (startTime != null && endTime != null) {
                    LocalDateTime previousStartTime = startTime.minusDays(java.time.temporal.ChronoUnit.DAYS.between(startTime, endTime));
                    LocalDateTime previousEndTime = startTime.minusDays(1);
                    List<ElectricityData> previousData = getElectricityDataByTimeRange(previousStartTime, previousEndTime);
                    
                    if (!previousData.isEmpty()) {
                        Map<LocalDate, Double> previousDailyConsumption = previousData.stream()
                            .filter(d -> d.getElectricityConsumption() != null)
                            .collect(Collectors.groupingBy(
                                d -> d.getRecordTime().toLocalDate(),
                                Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                            ));
                        
                        double previousTotalConsumption = previousDailyConsumption.values().stream().mapToDouble(Double::doubleValue).sum();
                        double previousAverageDailyUsage = previousDailyConsumption.size() > 0 ? previousTotalConsumption / previousDailyConsumption.size() : 0;
                        
                        if (previousAverageDailyUsage > 0) {
                            double monthOnMonthGrowth = ((averageDailyUsage - previousAverageDailyUsage) / previousAverageDailyUsage) * 100;
                            reportContent.append(String.format("用电量环比: %+.1f%% (较上一周期)\n", monthOnMonthGrowth));
                        }
                    }
                }
                
                
                if (startTime != null && endTime != null) {
                    LocalDateTime yearAgoStartTime = startTime.minusYears(1);
                    LocalDateTime yearAgoEndTime = endTime.minusYears(1);
                    List<ElectricityData> yearAgoData = getElectricityDataByTimeRange(yearAgoStartTime, yearAgoEndTime);
                    
                    if (!yearAgoData.isEmpty()) {
                        Map<LocalDate, Double> yearAgoDailyConsumption = yearAgoData.stream()
                            .filter(d -> d.getElectricityConsumption() != null)
                            .collect(Collectors.groupingBy(
                                d -> d.getRecordTime().toLocalDate(),
                                Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                            ));
                        
                        double yearAgoTotalConsumption = yearAgoDailyConsumption.values().stream().mapToDouble(Double::doubleValue).sum();
                        double yearAgoAverageDailyUsage = yearAgoDailyConsumption.size() > 0 ? yearAgoTotalConsumption / yearAgoDailyConsumption.size() : 0;
                        
                        if (yearAgoAverageDailyUsage > 0) {
                            double yearOnYearGrowth = ((averageDailyUsage - yearAgoAverageDailyUsage) / yearAgoAverageDailyUsage) * 100;
                            reportContent.append(String.format("用电量同比: %+.1f%% (较去年同期)\n", yearOnYearGrowth));
                        }
                    }
                }
                
                
                double standardDeviation = 0.0;
                double peakToValleyRatio = 0.0;
                Optional<Double> maxDailyConsumption = Optional.empty();
                Optional<Double> minDailyConsumption = Optional.empty();
                
                
                if (!dailyConsumption.isEmpty()) {
                    
                    double variance = dailyConsumption.values().stream()
                        .mapToDouble(value -> Math.pow(value - averageDailyUsage, 2))
                        .average()
                        .orElse(0.0);
                    standardDeviation = Math.sqrt(variance);
                    
                    String stabilityLevel = "较高";
                    if (standardDeviation > averageDailyUsage * 0.3) {
                        stabilityLevel = "较低";
                    } else if (standardDeviation > averageDailyUsage * 0.15) {
                        stabilityLevel = "一般";
                    }
                    
                    reportContent.append(String.format("用电稳定性: %s (日用电量标准差: %.1f kWh)\n", stabilityLevel, standardDeviation));
                    
                    
                    maxDailyConsumption = dailyConsumption.values().stream().max(Double::compareTo);
                    minDailyConsumption = dailyConsumption.values().stream().min(Double::compareTo);
                    
                    if (maxDailyConsumption.isPresent() && minDailyConsumption.isPresent() && minDailyConsumption.get() > 0) {
                        peakToValleyRatio = maxDailyConsumption.get() / minDailyConsumption.get();
                        reportContent.append(String.format("峰谷差率: %.2f (最高日用电量: %.1f kWh, 最低日用电量: %.1f kWh)\n", 
                                                         peakToValleyRatio, maxDailyConsumption.get(), minDailyConsumption.get()));
                    }
                }
                
                
                reportContent.append("\n--- 规律分析 ---\n");
                
                
                String peakHour = "19:00 - 22:00";
                String patternType = "综合型 (生活与商业混合)";
                if (averageDailyUsage > 50) {
                    patternType = "商业型 (高耗能)";
                } else if (averageDailyUsage < 10) {
                    patternType = "住宅型 (低耗能)";
                }
                
                reportContent.append("主要用电模式: ").append(patternType).append("\n");
                reportContent.append("主要高峰时段: ").append(peakHour).append("\n");
                
                
                reportContent.append("\n--- 风险提示 ---\n");
                
                
                List<AnomalyDetectionRecord> anomalyRecords = new ArrayList<>();
                if (userId != null) {
                    anomalyRecords = anomalyDetectionService.getAnomalyDetectionRecordsByUserId(userId);
                } else {
                    anomalyRecords = anomalyDetectionService.getAnomalyDetectionRecordsByTimeRange(startTime, endTime);
                }
                
                if (!anomalyRecords.isEmpty()) {
                    long powerAnomalies = anomalyRecords.stream()
                        .filter(r -> "HIGH_DEVIATION".equals(r.getAnomalyType()) || "功率异常".equals(r.getAnomalyType()))
                        .count();
                    
                    long patternAnomalies = anomalyRecords.stream()
                        .filter(r -> "PATTERN_DEVIATION".equals(r.getAnomalyType()) || "用电模式异常".equals(r.getAnomalyType()))
                        .count();
                    
                    reportContent.append(String.format("本周期异常告警: 共%d 次\n", anomalyRecords.size()));
                    if (powerAnomalies > 0) {
                        reportContent.append(String.format("- 功率超限: %d 次\n", powerAnomalies));
                    }
                    if (patternAnomalies > 0) {
                        reportContent.append(String.format("- 用电模式异常: %d 次\n", patternAnomalies));
                    }
                } else {
                    reportContent.append("本周期无异常告警\n");
                }
                
                
                reportContent.append("\n--- 系统建议 ---\n");
                
                if (standardDeviation > averageDailyUsage * 0.3) {
                    reportContent.append("> 用电量波动较大，建议关注设备运行状态，避免不必要的能源浪费。\n");
                }
                
                if (peakToValleyRatio > 3.0) {
                    reportContent.append("> 峰谷差率较高，建议错峰用电以降低电费支出。\n");
                }
                
                if (anomalyRecords.size() > 3) {
                    reportContent.append("> 异常告警频发，建议安排人员进行线路巡检或安全用电抽查。\n");
                }
                
                if (averageDailyUsage > 50) {
                    reportContent.append("> 用电量较大，建议定期检查设备能效，考虑节能改造。\n");
                }
                
                
                if (startTime != null && endTime != null) {
                    LocalDateTime previousStartTime = startTime.minusDays(java.time.temporal.ChronoUnit.DAYS.between(startTime, endTime));
                    LocalDateTime previousEndTime = startTime.minusDays(1);
                    List<ElectricityData> previousData = getElectricityDataByTimeRange(previousStartTime, previousEndTime);
                    
                    if (!previousData.isEmpty()) {
                        Map<LocalDate, Double> previousDailyConsumption = previousData.stream()
                            .filter(d -> d.getElectricityConsumption() != null)
                            .collect(Collectors.groupingBy(
                                d -> d.getRecordTime().toLocalDate(),
                                Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                            ));
                        
                        double previousTotalConsumption = previousDailyConsumption.values().stream().mapToDouble(Double::doubleValue).sum();
                        double previousAverageDailyUsage = previousDailyConsumption.size() > 0 ? previousTotalConsumption / previousDailyConsumption.size() : 0;
                        
                        if (previousAverageDailyUsage > 0) {
                            double monthOnMonthGrowth = ((averageDailyUsage - previousAverageDailyUsage) / previousAverageDailyUsage) * 100;
                            
                            if (monthOnMonthGrowth > 10) {
                                reportContent.append("> 检测到用电量环比增长超过10%，请检查是否有新增大功率电器或设备长时间运行。\n");
                            } else if (monthOnMonthGrowth < -10) {
                                reportContent.append("> 用电量环比下降超过10%，可能是节能措施见效或设备停用，建议确认设备运行状态。\n");
                            }
                        }
                    }
                    
                    
                    LocalDateTime yearAgoStartTime = startTime.minusYears(1);
                    LocalDateTime yearAgoEndTime = endTime.minusYears(1);
                    List<ElectricityData> yearAgoData = getElectricityDataByTimeRange(yearAgoStartTime, yearAgoEndTime);
                    
                    if (!yearAgoData.isEmpty()) {
                        Map<LocalDate, Double> yearAgoDailyConsumption = yearAgoData.stream()
                            .filter(d -> d.getElectricityConsumption() != null)
                            .collect(Collectors.groupingBy(
                                d -> d.getRecordTime().toLocalDate(),
                                Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                            ));
                        
                        double yearAgoTotalConsumption = yearAgoDailyConsumption.values().stream().mapToDouble(Double::doubleValue).sum();
                        double yearAgoAverageDailyUsage = yearAgoDailyConsumption.size() > 0 ? yearAgoTotalConsumption / yearAgoDailyConsumption.size() : 0;
                        
                        if (yearAgoAverageDailyUsage > 0) {
                            double yearOnYearGrowth = ((averageDailyUsage - yearAgoAverageDailyUsage) / yearAgoAverageDailyUsage) * 100;
                            
                            if (yearOnYearGrowth > 5) {
                                reportContent.append("> 用电量同比增长超过5%，可能反映了业务扩张或季节性因素影响。\n");
                            } else if (yearOnYearGrowth < -5) {
                                reportContent.append("> 用电量同比下降超过5%，可能反映了节能措施成效或业务规模缩减。\n");
                            }
                        }
                    }
                }
                
                
                if (averageDailyUsage > 50) {
                    reportContent.append("> 作为高耗能用户，建议考虑安装分时电表，利用谷时段电价优势降低用电成本。\n");
                } else if (averageDailyUsage < 10) {
                    reportContent.append("> 作为低耗能用户，可以考虑安装太阳能等可再生能源设备，进一步降低能源成本。\n");
                }
                
                
                if (!anomalyRecords.isEmpty()) {
                    long powerAnomalies = anomalyRecords.stream()
                        .filter(r -> "HIGH_DEVIATION".equals(r.getAnomalyType()) || "功率异常".equals(r.getAnomalyType()))
                        .count();
                    
                    long patternAnomalies = anomalyRecords.stream()
                        .filter(r -> "PATTERN_DEVIATION".equals(r.getAnomalyType()) || "用电模式异常".equals(r.getAnomalyType()))
                        .count();
                    
                    if (powerAnomalies > 0) {
                        reportContent.append("> 检测到功率超限告警，建议检查变压器容量和负载分配，避免过载运行。\n");
                    }
                    
                    if (patternAnomalies > 0) {
                        reportContent.append("> 检测到用电模式异常，可能存在窃电或线路故障风险，建议进行现场核查。\n");
                    }
                }
                
                
                reportContent.append("\n--- 节能建议 ---\n");
                
                if (peakToValleyRatio > 2.0) {
                    reportContent.append("> 建议实施错峰用电管理，将非紧急用电任务安排至低谷时段，可降低约15-20%的用电成本。\n");
                }
                
                if (standardDeviation > averageDailyUsage * 0.2) {
                    reportContent.append("> 建议建立用电监控机制，及时发现异常用电行为，提高能源利用效率。\n");
                }
                
                
                int month = LocalDate.now().getMonthValue();
                if (month >= 6 && month <= 8) {
                    reportContent.append("> 夏季高峰期，建议优化空调运行策略，设定温度不低于26℃，可节省约10%的制冷能耗。\n");
                } else if (month >= 12 || month <= 2) {
                    reportContent.append("> 冬季供暖期，建议加强保温措施，合理设定供暖温度，避免能源浪费。\n");
                }
                
                reportContent.append("> 定期进行能源审计，识别节能潜力，制定长期节能计划。\n");
                reportContent.append("> 考虑采用智能能源管理系统，实现精细化用电管理。\n");
            } else {
                reportContent.append("数据统计:\n");
                reportContent.append("总记录数: ").append(data.size()).append("\n");
                reportContent.append("无有效用电数据\n");
            }
            
            outputStream.write(reportContent.toString().getBytes("UTF-8"));
            
            
            reportStorage.put(reportId, outputStream.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("生成报告失败", e);
        }
        
        return reportId;
    }

    @Override
    public byte[] exportData(Long userId, LocalDateTime startTime, LocalDateTime endTime, String format) {
        try {
            List<ElectricityData> data;
            if (userId != null && startTime != null && endTime != null) {
                data = getElectricityDataByUserIdAndTimeRange(userId, startTime, endTime);
            } else if (userId != null) {
                data = getElectricityDataByUserId(userId);
            } else if (startTime != null && endTime != null) {
                data = getElectricityDataByTimeRange(startTime, endTime);
            } else {
                data = getAllElectricityData();
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            if ("CSV".equalsIgnoreCase(format)) {
                outputStream.write("ID,用户ID,记录时间,用电量(kWh)\n".getBytes("UTF-8"));
                for (ElectricityData item : data) {
                    String line = String.format("%d,%d,%s,%.2f\n",
                            item.getId(),
                            item.getUser() != null ? item.getUser().getId() : 0,
                            item.getRecordTime(),
                            item.getElectricityConsumption() != null ? item.getElectricityConsumption() : 0);
                    outputStream.write(line.getBytes("UTF-8"));
                }
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                
                outputStream.write("\uFEFF".getBytes("UTF-8")); 
                outputStream.write("ID,用户ID,记录时间,用电量(kWh)\n".getBytes("UTF-8"));
                for (ElectricityData item : data) {
                    String line = String.format("%d,%d,%s,%.2f\n",
                            item.getId(),
                            item.getUser() != null ? item.getUser().getId() : 0,
                            item.getRecordTime(),
                            item.getElectricityConsumption() != null ? item.getElectricityConsumption() : 0);
                    outputStream.write(line.getBytes("UTF-8"));
                }
            } else {
                
                outputStream.write("ID,用户ID,记录时间,用电量(kWh)\n".getBytes("UTF-8"));
                for (ElectricityData item : data) {
                    String line = String.format("%d,%d,%s,%.2f\n",
                            item.getId(),
                            item.getUser() != null ? item.getUser().getId() : 0,
                            item.getRecordTime(),
                            item.getElectricityConsumption() != null ? item.getElectricityConsumption() : 0);
                    outputStream.write(line.getBytes("UTF-8"));
                }
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出数据失败", e);
        }
    }
    
    @Override
    public byte[] downloadReport(String reportId) {
        byte[] reportContent = reportStorage.get(reportId);
        if (reportContent == null) {
            throw new RuntimeException("报告不存在或已过期");
        }
        return reportContent;
    }
    
    @Override
    public byte[] exportAnalysisData(Long userId, String period, String format) {
        try {
            
            int days = parsePeriodToDays(period);
            
            
            List<ElectricityData> data;
            if (userId != null) {
                data = getRecentElectricityDataByUserId(userId, days);
            } else {
                LocalDateTime startDate = LocalDateTime.now().minusDays(days);
                data = getElectricityDataByTimeRange(startDate, LocalDateTime.now());
            }
            
            
            Map<String, Double> dailyStats = data.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getRecordTime().toLocalDate().toString(),
                    Collectors.summingDouble(item -> item.getElectricityConsumption() != null ? item.getElectricityConsumption() : 0)
                ));
            
            
            double totalConsumption = dailyStats.values().stream().mapToDouble(Double::doubleValue).sum();
            double averageDailyUsage = dailyStats.size() > 0 ? totalConsumption / dailyStats.size() : 0;
            
            
            double predictedMonthlyUsage = averageDailyUsage * 30;
            
            
            String peakHour = averageDailyUsage > 0 ? "19:00-22:00" : "无数据";
            
            
            String patternType = "未分析";
            if (averageDailyUsage > 0) {
                if (userId != null) {
                    patternType = "住宅型";
                } else {
                    patternType = "综合型";
                }
            }
            
            
            List<Map<String, Object>> analysisResults = new ArrayList<>();
            
            if (userId != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("userId", userId);
                result.put("username", "用户" + userId); 
                result.put("averageDailyUsage", Math.round(averageDailyUsage * 10) / 10.0);
                result.put("peakHour", peakHour);
                result.put("patternType", patternType);
                result.put("predictedMonthlyUsage", Math.round(predictedMonthlyUsage * 10) / 10.0);
                result.put("analysisDate", LocalDate.now().toString());
                analysisResults.add(result);
            } else {
                
                Map<String, Object> result = new HashMap<>();
                result.put("userId", 0);
                result.put("username", "全体用户");
                result.put("averageDailyUsage", Math.round(averageDailyUsage * 10) / 10.0);
                result.put("peakHour", peakHour);
                result.put("patternType", patternType);
                result.put("predictedMonthlyUsage", Math.round(predictedMonthlyUsage * 10) / 10.0);
                result.put("analysisDate", LocalDate.now().toString());
                analysisResults.add(result);
            }
            
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            if ("EXCEL".equalsIgnoreCase(format)) {
                
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("用电规律分析数据");
                
                
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                
                
                Row headerRow = sheet.createRow(0);
                String[] headers = {"用户ID", "用户名", "平均日用电量(kWh)", "用电高峰时段", "用电规律类型", "预测月用电量(kWh)", "分析日期"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                
                int rowNum = 1;
                for (Map<String, Object> result : analysisResults) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue((Integer) result.get("userId"));
                    row.createCell(1).setCellValue((String) result.get("username"));
                    row.createCell(2).setCellValue((Double) result.get("averageDailyUsage"));
                    row.createCell(3).setCellValue((String) result.get("peakHour"));
                    row.createCell(4).setCellValue((String) result.get("patternType"));
                    row.createCell(5).setCellValue((Double) result.get("predictedMonthlyUsage"));
                    row.createCell(6).setCellValue((String) result.get("analysisDate"));
                }
                
                
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                
                workbook.write(outputStream);
                workbook.close();
            } else {
                
                outputStream.write("\uFEFF".getBytes("UTF-8")); 
                outputStream.write("用户ID,用户名,平均日用电量(kWh),用电高峰时段,用电规律类型,预测月用电量(kWh),分析日期\n".getBytes("UTF-8"));
                for (Map<String, Object> result : analysisResults) {
                    String line = String.format("%d,%s,%.1f,%s,%s,%.1f,%s\n",
                            result.get("userId"),
                            result.get("username"),
                            result.get("averageDailyUsage"),
                            result.get("peakHour"),
                            result.get("patternType"),
                            result.get("predictedMonthlyUsage"),
                            result.get("analysisDate"));
                    outputStream.write(line.getBytes("UTF-8"));
                }
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出分析数据失败", e);
        }
    }
    
    private int parsePeriodToDays(String period) {
        if (period == null) return 30;
        try {
            if (period.endsWith("d")) {
                return Integer.parseInt(period.substring(0, period.length() - 1));
            } else if (period.endsWith("m")) {
                return Integer.parseInt(period.substring(0, period.length() - 1)) * 30;
            } else {
                return Integer.parseInt(period);
            }
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}