package com.powermanage.service;

import com.powermanage.entity.ElectricityData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ElectricityDataService {
    
    
    ElectricityData saveElectricityData(ElectricityData electricityData);
    
    
    List<ElectricityData> getElectricityDataByUserId(Long userId);
    
    
    List<ElectricityData> getElectricityDataByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    
    List<ElectricityData> getElectricityDataByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    
    List<ElectricityData> getRecentElectricityDataByUserId(Long userId, int days);
    
    
    List<ElectricityData> batchSaveElectricityData(List<ElectricityData> electricityDataList);
    
    
    List<ElectricityData> getAllElectricityData();
    
    
    ElectricityData getElectricityDataById(Long id);
    
    
    void deleteElectricityData(Long id);
    
    
    ElectricityData updateElectricityData(ElectricityData electricityData);
    
    
    List<ElectricityData> getRecentElectricityData(int count);
    
    
    double getTotalElectricityConsumptionByDate(LocalDate date);
    
    
    byte[] generateUserReport(Long userId);
    
    
    String generateReport(Long userId, LocalDateTime startTime, LocalDateTime endTime, String reportType);
    
    
    byte[] exportData(Long userId, LocalDateTime startTime, LocalDateTime endTime, String format);
    
    byte[] downloadReport(String reportId);
    
    byte[] exportAnalysisData(Long userId, String period, String format);
}