package com.powermanage.service;

import com.powermanage.entity.AnomalyDetectionRecord;
import com.powermanage.entity.ElectricityData;
import com.powermanage.entity.UserElectricityPattern;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnomalyDetectionService {

    
    boolean detectAnomaly(ElectricityData electricityData);

    
    List<AnomalyDetectionRecord> batchDetectAnomalies(List<ElectricityData> electricityDataList);

    
    List<AnomalyDetectionRecord> detectAnomaliesBasedOnPattern(Long userId, List<ElectricityData> electricityDataList);

    
    List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByUserId(Long userId);

    
    List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    
    List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByAnomalyType(String anomalyType);

    
    List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByAlgorithm(String algorithm);

    
    void updateDetectionAlgorithmParameters(String algorithm, String parameters);

    
    UserElectricityPattern buildUserElectricityPattern(Long userId);

    
    UserElectricityPattern getUserElectricityPattern(Long userId, String version);

    
    List<UserElectricityPattern> batchBuildUserElectricityPatterns(List<Long> userIds);

    
    void generateAnomalyStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    
    long getAnomalyCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    
    long getProcessedAnomalyCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 扫描指定时间之后的历史数据，对未检测的数据进行补测
     * @param since 起始时间
     */
    void scanAndDetectUnprocessedData(LocalDateTime since);
    
    /**
     * 检查并为缺失画像的用户构建画像
     */
    void checkAndBuildMissingPatterns();

    List<Map<String, Object>> getRecentAnomalies(int limit);

    
    List<Map<String, Object>> getAnomalyTypeDistribution();
}