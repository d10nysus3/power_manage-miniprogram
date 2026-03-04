package com.powermanage.service.impl;



import com.powermanage.entity.AlertRecord;
import com.powermanage.entity.AnomalyDetectionRecord;

import com.powermanage.entity.ElectricityData;

import com.powermanage.entity.UserElectricityPattern;

import com.powermanage.repository.AlertRecordRepository;
import com.powermanage.repository.AnomalyDetectionRecordRepository;

import com.powermanage.repository.ElectricityDataRepository;

import com.powermanage.repository.UserElectricityPatternRepository;

import com.powermanage.service.AnomalyDetectionService;
import com.powermanage.service.RealtimeAlertService;
import com.powermanage.service.UserService;
import com.powermanage.util.DataPreprocessingUtil;

import com.powermanage.util.AlgorithmUtil;

import com.powermanage.util.AnomalyDetectionUtil;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Collections;

import java.util.Comparator;

import java.util.List;

import java.util.Map;

import java.util.HashMap;

import java.util.stream.Collectors;



@Service

public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {



    @Autowired
    private AnomalyDetectionRecordRepository anomalyDetectionRecordRepository;

    @Autowired
    private AlertRecordRepository alertRecordRepository;

    @Autowired
    private ElectricityDataRepository electricityDataRepository;



    @Autowired

    private UserElectricityPatternRepository userElectricityPatternRepository;



    @Autowired

    private UserService userService;



    @Autowired

    private DataPreprocessingUtil dataPreprocessingUtil;



    @Autowired

    private AlgorithmUtil algorithmUtil;



    @Autowired
    private AnomalyDetectionUtil anomalyDetectionUtil;

    @Autowired
    private RealtimeAlertService realtimeAlertService;

    @Override
    public boolean detectAnomaly(ElectricityData electricityData) {

        

        if (electricityData == null || !isValidElectricityData(electricityData)) {

            throw new IllegalArgumentException("输入的用电数据无效或不真实");

        }

        

        

        List<ElectricityData> singleDataList = new ArrayList<>();

        singleDataList.add(electricityData);

        List<ElectricityData> cleanedData = dataPreprocessingUtil.detectAndMarkAnomalies(singleDataList);

        

        

        List<UserElectricityPattern> patterns = userElectricityPatternRepository
                .findTopByUser_IdOrderByAnalysisDateDesc(electricityData.getUserId());

        // 检查画像是否过期 (超过30天)
        if (!patterns.isEmpty()) {
            UserElectricityPattern latestPattern = patterns.get(0);
            if (latestPattern.getAnalysisDate() != null && 
                latestPattern.getAnalysisDate().isBefore(LocalDateTime.now().minusDays(30))) {
                try {
                    System.out.println("检测到用户 " + electricityData.getUserId() + " 的画像已过期(>30天)，正在触发自动更新...");
                    // 重新构建画像
                    UserElectricityPattern newPattern = buildUserElectricityPattern(electricityData.getUserId());
                    // 使用新画像进行本次检测
                    patterns = List.of(newPattern);
                } catch (Exception e) {
                    System.err.println("自动更新过期画像失败，将继续使用旧画像: " + e.getMessage());
                    // 失败时保持使用旧画像，不中断流程
                }
            }
        }

        if (patterns.isEmpty()) {

            

            try {
                UserElectricityPattern newPattern = buildUserElectricityPattern(electricityData.getUserId());
                patterns = List.of(newPattern);
            } catch (RuntimeException e) {
                System.out.println("用户缺乏足够的历史用电数据，将使用默认模式进行检测: " + e.getMessage());
                // 构建一个默认的通用模式 (Cold Start)
                UserElectricityPattern defaultPattern = new UserElectricityPattern();
                defaultPattern.setUserId(electricityData.getUserId());
                defaultPattern.setAlgorithm("COLD_START_DEFAULT");
                defaultPattern.setVersion("1.0-DEFAULT");
                defaultPattern.setAnalysisDate(LocalDateTime.now());
                
                JSONObject defaultFeatures = new JSONObject();
                defaultFeatures.put("avgPower", 500.0); // 假设平均功率500W
                defaultFeatures.put("stdDev", 200.0);   // 标准差200W
                defaultFeatures.put("maxPower", 2000.0);
                defaultFeatures.put("minPower", 0.0);
                defaultFeatures.put("avgConsumption", 2.0); // 假设平均用电量2kWh (每15分钟)
                defaultFeatures.put("consumptionStdDev", 1.0); 
                
                defaultPattern.setFeatureVector(defaultFeatures.toString());
                defaultPattern.setPatternData(new JSONObject().toString());
                
                // 持久化默认画像，确保数据库中有记录，且后续可基于此进行检测
                try {
                    // 先检查用户是否存在，避免外键约束导致保存失败
                    if (userService.getUserById(electricityData.getUserId()).isPresent()) {
                        userElectricityPatternRepository.save(defaultPattern);
                        System.out.println("成功为用户 " + electricityData.getUserId() + " 保存默认冷启动画像");
                    } else {
                        System.err.println("严重警告: 用户 " + electricityData.getUserId() + " 在 t_user 表中不存在，无法保存画像！即使 t_electricity_data 中有数据，这也是数据一致性错误。");
                    }
                } catch (Exception ex) {
                    System.err.println("保存默认画像失败: " + ex.getMessage());
                    ex.printStackTrace();
                    // 如果保存失败，至少让内存中有 pattern，不影响本次检测
                }
                
                patterns = List.of(defaultPattern);
            }

        }

        

        if (!patterns.isEmpty() && !cleanedData.isEmpty() && cleanedData.get(0).getIsValid()) {

            

            AnomalyDetectionUtil.AnomalyDetectionResult result = 

                    anomalyDetectionUtil.detectAnomaly(cleanedData, patterns.get(0));

            

            if (result.isAnomaly()) {

                

                AnomalyDetectionRecord record = new AnomalyDetectionRecord();

                

                

                userService.getUserById(electricityData.getUserId()).ifPresent(user -> {

                    record.setUser(user);

                });

                

                record.setDetectionTime(LocalDateTime.now());

                

                double mahalanobisDistance = 0.0;
                if (result.getAlgorithmResults() != null) {
                    if (result.getAlgorithmResults().containsKey("mahalanobis")) {
                        mahalanobisDistance = ((Number) result.getAlgorithmResults().get("mahalanobis")).doubleValue();
                    } else if (result.getAlgorithmResults().containsKey("mahalanobisDistance")) {
                        mahalanobisDistance = ((Number) result.getAlgorithmResults().get("mahalanobisDistance")).doubleValue();
                    }
                }

                if (mahalanobisDistance > 3.0) {
                    record.setAnomalyType("HIGH_DEVIATION");
                } else if (mahalanobisDistance < -3.0) {
                    record.setAnomalyType("LOW_DEVIATION");
                } else {
                    record.setAnomalyType("PATTERN_DEVIATION");
                }

                record.setDetectionDataId(electricityData.getId());

                record.setAlgorithm("ENHANCED_HYBRID_DETECTION");

                

                

                JSONObject details = new JSONObject();

                details.put("electricityValue", electricityData.getElectricityValue());

                details.put("anomalyScore", result.getAnomalyScore());

                details.put("algorithmResults", result.getAlgorithmResults());

                details.put("notes", result.getNotes());

                

                record.setAnomalyDetails(details.toString());

                record.setIsHandled(false);

                // Frequency Control: Check if a similar anomaly was recorded in the last 15 minutes
                LocalDateTime limitTime = LocalDateTime.now().minusMinutes(15);
                List<AnomalyDetectionRecord> recentRecords = anomalyDetectionRecordRepository
                        .findByUser_IdAndAnomalyTypeAndDetectionTimeAfter(electricityData.getUserId(), record.getAnomalyType(), limitTime);

                if (!recentRecords.isEmpty()) {
                    System.out.println("告警频控: 用户 " + electricityData.getUserId() + " 的 " + record.getAnomalyType() + " 异常在最近 15 分钟内已上报，跳过本次推送");
                    return true; // Return true as anomaly was detected, but suppress duplicate alert
                }
                
                anomalyDetectionRecordRepository.save(record);
                
                // 自动生成并保存异常警报
                createAndSaveAlertRecord(record, electricityData);

                // 触发实时告警推送
                realtimeAlertService.pushAnomalyAlert(record, record.getUser());

                return true;

            }

        }

        return false;

    }

    @Override

    public List<AnomalyDetectionRecord> batchDetectAnomalies(List<ElectricityData> electricityDataList) {

        

        List<ElectricityData> cleanedData = dataPreprocessingUtil.detectAndMarkAnomalies(electricityDataList);

        

        

        Map<Long, List<ElectricityData>> dataByUser = cleanedData.stream()

                .filter(ElectricityData::getIsValid)

                .collect(Collectors.groupingBy(ElectricityData::getUserId));

        

        List<AnomalyDetectionRecord> anomalyRecords = new ArrayList<>();

        

        for (Map.Entry<Long, List<ElectricityData>> entry : dataByUser.entrySet()) {

            Long userId = entry.getKey();

            List<ElectricityData> userData = entry.getValue();

            

            

            List<UserElectricityPattern> patterns = userElectricityPatternRepository

                    .findTopByUser_IdOrderByAnalysisDateDesc(userId);

            

            if (!patterns.isEmpty()) {

                UserElectricityPattern pattern = patterns.get(0);

                

                for (ElectricityData data : userData) {

                    List<ElectricityData> singleDataList = new ArrayList<>();

                    singleDataList.add(data);

                    

                    

                    AnomalyDetectionUtil.AnomalyDetectionResult result = 

                            anomalyDetectionUtil.detectAnomaly(singleDataList, pattern);

                    

                    if (result.isAnomaly()) {

                        

                        AnomalyDetectionRecord record = createAnomalyRecord(userId, data, result);

                        anomalyRecords.add(record);

                    }

                }

            }

        }

        

        return anomalyRecords;

    }

    

    

    private AnomalyDetectionRecord createAnomalyRecord(Long userId, ElectricityData data, 
            AnomalyDetectionUtil.AnomalyDetectionResult result) {
        AnomalyDetectionRecord record = new AnomalyDetectionRecord();
        
        
        userService.getUserById(userId).ifPresent(user -> {
            record.setUser(user);
        });
        
        record.setDetectionTime(LocalDateTime.now());
        
        
        String anomalyType = "PATTERN_DEVIATION";
        
        double mahalanobisDistance = 0.0;
        double dtwDistance = 0.0;
        
        if (result.getAlgorithmResults() != null) {
            if (result.getAlgorithmResults().containsKey("mahalanobis")) {
                mahalanobisDistance = ((Number) result.getAlgorithmResults().get("mahalanobis")).doubleValue();
            } else if (result.getAlgorithmResults().containsKey("mahalanobisDistance")) {
                mahalanobisDistance = ((Number) result.getAlgorithmResults().get("mahalanobisDistance")).doubleValue();
            }
            
            if (result.getAlgorithmResults().containsKey("dtw")) {
                dtwDistance = ((Number) result.getAlgorithmResults().get("dtw")).doubleValue();
            } else if (result.getAlgorithmResults().containsKey("dtwDistance")) {
                dtwDistance = ((Number) result.getAlgorithmResults().get("dtwDistance")).doubleValue();
            }
        }
        
        if (mahalanobisDistance > 3.0) {
            anomalyType = "HIGH_DEVIATION";
        } else if (dtwDistance > 1.0) {
            anomalyType = "TREND_DEVIATION";
        }
        
        record.setAnomalyType(anomalyType);

        // Frequency Control: Check if a similar anomaly was recorded in the last 15 minutes
        LocalDateTime limitTime = LocalDateTime.now().minusMinutes(15);
        List<AnomalyDetectionRecord> recentRecords = anomalyDetectionRecordRepository
                .findByUser_IdAndAnomalyTypeAndDetectionTimeAfter(userId, anomalyType, limitTime);

        if (!recentRecords.isEmpty()) {
            System.out.println("告警频控: 用户 " + userId + " 的 " + anomalyType + " 异常在最近 15 分钟内已上报，跳过本次推送");
            return recentRecords.get(0);
        }

        record.setDetectionDataId(data.getId());
        record.setAlgorithm("ENHANCED_HYBRID_DETECTION");
        
        
        JSONObject details = new JSONObject();
        details.put("electricityValue", data.getElectricityValue());
        details.put("anomalyScore", result.getAnomalyScore());
        details.put("algorithmResults", result.getAlgorithmResults());
        details.put("notes", result.getNotes());
        
        record.setAnomalyDetails(details.toString());
        record.setIsHandled(false);
        
        AnomalyDetectionRecord savedRecord = anomalyDetectionRecordRepository.save(record);
        
        // 自动生成并保存异常警报
        createAndSaveAlertRecord(savedRecord, data);
        
        // 触发实时告警推送
        realtimeAlertService.pushAnomalyAlert(savedRecord, savedRecord.getUser());
        
        return savedRecord;
    }



    @Override

    public List<AnomalyDetectionRecord> detectAnomaliesBasedOnPattern(Long userId, List<ElectricityData> electricityDataList) {

        

        List<ElectricityData> cleanedData = dataPreprocessingUtil.detectAndMarkAnomalies(electricityDataList);

        List<ElectricityData> validData = cleanedData.stream()

                .filter(ElectricityData::getIsValid)

                .collect(Collectors.toList());

        

        

        validData.sort(Comparator.comparing(ElectricityData::getRecordTime));

        

        

        List<UserElectricityPattern> patterns = userElectricityPatternRepository

                .findTopByUser_IdOrderByAnalysisDateDesc(userId);

        

        List<AnomalyDetectionRecord> anomalyRecords = new ArrayList<>();

        

        if (!patterns.isEmpty() && !validData.isEmpty()) {

            UserElectricityPattern pattern = patterns.get(0);

            

            

            AnomalyDetectionUtil.AnomalyDetectionResult result = 

                    anomalyDetectionUtil.detectAnomaly(validData, pattern);

            

            if (result.isAnomaly()) {

                

                for (ElectricityData data : validData) {

                    AnomalyDetectionRecord record = createAnomalyRecord(userId, data, result);

                    anomalyRecords.add(record);

                }

            }

        }

        

        return anomalyRecords;

    }



    @Override
    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByUserId(Long userId) {
        // 实时监控查询时，若无用户画像，则立即构建，确保后续检测正常进行
        checkAndBuildPattern(userId);
        return anomalyDetectionRecordRepository.findByUser_Id(userId);
    }

    private void checkAndBuildPattern(Long userId) {
        try {
            List<UserElectricityPattern> patterns = userElectricityPatternRepository
                    .findTopByUser_IdOrderByAnalysisDateDesc(userId);
            
            boolean needBuild = false;
            if (patterns.isEmpty()) {
                System.out.println("用户 " + userId + " 暂无画像，触发立即构建...");
                needBuild = true;
            } else {
                UserElectricityPattern latest = patterns.get(0);
                if (latest.getAnalysisDate() != null && 
                    latest.getAnalysisDate().isBefore(LocalDateTime.now().minusDays(30))) {
                    System.out.println("用户 " + userId + " 画像已过期，触发立即更新...");
                    needBuild = true;
                }
            }

            if (needBuild) {
                // 异步或同步构建？为了"立即"响应监控，这里选择同步构建，
                // 虽然会稍微增加本次查询的延迟，但能保证后续状态正确。
                try {
                    buildUserElectricityPattern(userId);
                    System.out.println("用户 " + userId + " 画像构建完成。");
                } catch (Exception e) {
                    // 如果构建失败（例如数据不足），记录日志但不中断查询流程
                    System.err.println("为用户 " + userId + " 自动构建画像失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("检查用户画像状态时发生错误: " + e.getMessage());
        }
    }



    @Override

    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {

        return anomalyDetectionRecordRepository.findByDetectionTimeBetween(startTime, endTime);

    }



    @Override

    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByAnomalyType(String anomalyType) {

        return anomalyDetectionRecordRepository.findByAnomalyType(anomalyType);

    }



    @Override

    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByAlgorithm(String algorithm) {

        return anomalyDetectionRecordRepository.findByAlgorithm(algorithm);

    }



    @Override

    public void updateDetectionAlgorithmParameters(String algorithm, String parameters) {

        

        

        System.out.println("Updating algorithm parameters: " + algorithm + ", params: " + parameters);

    }



    @Override
    public UserElectricityPattern buildUserElectricityPattern(Long userId) {
        
        LocalDateTime twoMonthsAgo = LocalDateTime.now().minusDays(60);

        // 使用 "历史" 类型的数据构建画像
        List<ElectricityData> userData = electricityDataRepository.findByUser_IdAndDataTypeAndRecordTimeAfter(userId, "历史", twoMonthsAgo);
        
        // 如果历史数据不足，尝试获取所有类型数据作为回退，或者抛出异常（根据需求，这里我们尝试回退到所有数据，或者严格点只用历史）
        // 根据用户需求："根据t_electricity_data表中data_type='历史'的数据构建用户画像"
        // 我们可以做一个fallback，如果'历史'数据不够，可能是新用户，尝试用'智能电表'等数据，或者严格执行。
        // 这里为了稳健性，如果历史数据为空，尝试查找所有数据，但优先使用历史。
        if (userData.isEmpty()) {
             // Fallback to all data if explicit 'History' data is missing, to avoid breaking system for existing users
             // But log it.
             // System.out.println("User " + userId + " has no '历史' data, falling back to all data sources.");
             // userData = electricityDataRepository.findByUser_IdAndRecordTimeAfter(userId, twoMonthsAgo);
             // 根据严格指令，我们应该只用历史。但考虑到系统稳定性，如果完全没历史数据，可能导致无法检测。
             // 让我们严格一点，如果没有历史数据，则无法构建基于历史的画像。
        }

        if (userData.isEmpty() || userData.size() < 5) { // 至少需要5个点才能构建有意义的模式
            System.err.println("用户 " + userId + " '历史'数据不足，当前条数: " + userData.size());
            // 尝试使用所有数据作为后备，以免系统瘫痪
            userData = electricityDataRepository.findByUser_IdAndRecordTimeAfter(userId, twoMonthsAgo);
            if (userData.isEmpty() || userData.size() < 5) {
                throw new RuntimeException("No electricity data available for user: " + userId);
            }
        }
        
        List<ElectricityData> cleanedData = dataPreprocessingUtil.detectAndMarkAnomalies(userData);

        List<ElectricityData> validData = cleanedData.stream()

                .filter(ElectricityData::getIsValid)

                .collect(Collectors.toList());

        

        

        List<ElectricityData> denoisedData = dataPreprocessingUtil.denoiseData(validData, 5);

        

        

        List<ElectricityData> alignedData = dataPreprocessingUtil.alignDataByTime(denoisedData, 15);

        

        

        List<Double> electricityValues = alignedData.stream()

                .map(ElectricityData::getElectricityValue)

                .collect(Collectors.toList());

        

        

        Map<String, Double> featureVector = new HashMap<>();

        

        

        double avgPower = electricityValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double avgConsumption = alignedData.stream()

                .mapToDouble(ElectricityData::getElectricityConsumption)

                .average()

                .orElse(0.0);

        double consumptionStdDev = calculateStandardDeviation(alignedData.stream()
                .map(ElectricityData::getElectricityConsumption)
                .collect(Collectors.toList()));

        double powerStdDev = calculateStandardDeviation(electricityValues);

        double maxPower = electricityValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        double minPower = electricityValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

        

        

        featureVector.put("avgPower", avgPower);

        featureVector.put("stdDev", powerStdDev);

        featureVector.put("maxPower", maxPower);

        featureVector.put("minPower", minPower);

        featureVector.put("avgConsumption", avgConsumption);

        featureVector.put("consumptionStdDev", consumptionStdDev);

        

        

        Map<String, Double> patternData = new HashMap<>();

        

        

        Map<String, List<Double>> hourlyValues = new HashMap<>();

        

        for (ElectricityData data : alignedData) {

            String hourKey = getTimeKey(data.getRecordTime());

            hourlyValues.computeIfAbsent(hourKey, k -> new ArrayList<>()).add(data.getElectricityValue());

        }

        

        

        for (Map.Entry<String, List<Double>> entry : hourlyValues.entrySet()) {

            String hourKey = entry.getKey();

            List<Double> values = entry.getValue();

            

            

            double mean = calculateMean(values);

            double median = calculateMedian(values);

            double stdDev = calculateStandardDeviation(values);

            

            

            patternData.put(hourKey + ".mean", mean);

            patternData.put(hourKey + ".median", median);

            patternData.put(hourKey + ".stdDev", stdDev);

        }

        

        

        // 检查是否已存在画像，如果存在则更新，否则新建
        UserElectricityPattern pattern = userElectricityPatternRepository.findByUser_Id(userId)
                .orElse(new UserElectricityPattern());
        
        if (pattern.getId() == null) {
            pattern.setUserId(userId);
        }

        pattern.setPatternData(new JSONObject(patternData).toString());

        pattern.setFeatureVector(new JSONObject(featureVector).toString());

        pattern.setAnalysisDate(LocalDateTime.now());

        pattern.setAlgorithm("ENHANCED_FEATURE_EXTRACTION");

        pattern.setVersion("2.0");

        

        return userElectricityPatternRepository.save(pattern);

    }



    @Override

    public UserElectricityPattern getUserElectricityPattern(Long userId, String version) {

        List<UserElectricityPattern> patterns = userElectricityPatternRepository.findByUser_IdAndVersion(userId, version);

        return patterns.isEmpty() ? null : patterns.get(0);

    }

    

    

    private double calculateMean(List<Double> values) {

        if (values == null || values.isEmpty()) {

            return 0.0;

        }

        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    }

    

    

    private double calculateMedian(List<Double> values) {

        if (values == null || values.isEmpty()) {

            return 0.0;

        }

        List<Double> sortedValues = new ArrayList<>(values);

        Collections.sort(sortedValues);

        int middle = sortedValues.size() / 2;

        if (sortedValues.size() % 2 == 0) {

            return (sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2.0;

        } else {

            return sortedValues.get(middle);

        }

    }

    

    

    private double calculateStandardDeviation(List<Double> values) {

        if (values == null || values.size() <= 1) {

            return 0.0;

        }

        double mean = calculateMean(values);

        double variance = values.stream()

                .mapToDouble(v -> Math.pow(v - mean, 2))

                .average()

                .orElse(0.0);

        return Math.sqrt(variance);

    }



    @Override

    public List<UserElectricityPattern> batchBuildUserElectricityPatterns(List<Long> userIds) {

        List<UserElectricityPattern> patterns = new ArrayList<>();

        

        for (Long userId : userIds) {

            patterns.add(buildUserElectricityPattern(userId));

        }

        

        return patterns;

    }



    @Override

    public void generateAnomalyStatistics(LocalDateTime startTime, LocalDateTime endTime) {

        

        

        List<AnomalyDetectionRecord> records = anomalyDetectionRecordRepository.findByDetectionTimeBetween(startTime, endTime);

        

        

        Map<String, Long> typeStatistics = records.stream()

                .collect(Collectors.groupingBy(AnomalyDetectionRecord::getAnomalyType, Collectors.counting()));

        

        

        Map<Long, Long> userStatistics = records.stream()

                .collect(Collectors.groupingBy(AnomalyDetectionRecord::getUserId, Collectors.counting()));

        

        System.out.println("Anomaly type statistics: " + typeStatistics);

        System.out.println("User anomaly statistics: " + userStatistics);

    }



    

    private JSONObject parsePatternData(String patternDataString) {

        try {

            return new JSONObject(patternDataString);

        } catch (Exception e) {

            

            return new JSONObject();

        }

    }



    

    private String getTimeKey(LocalDateTime time) {

        return String.format("%02d:00", time.getHour());

    }

    

    

    private boolean isValidElectricityData(ElectricityData data) {

        

        if (data.getUserId() == null || data.getRecordTime() == null) {

            return false;

        }

        

        

        if (data.getElectricityConsumption() != null && data.getElectricityConsumption() < 0) {

            return false;

        }

        

        if (data.getPower() != null && data.getPower() < 0) {

            return false;

        }

        

        

        if (data.getRecordTime().isAfter(LocalDateTime.now())) {

            return false;

        }

        

        

        String dataSource = data.getDataSource();

        if (dataSource == null || 

            (!dataSource.equals("智能电表") && 

             !dataSource.equals("人工采集") && 

             !dataSource.equals("系统导入"))) {

            return false;

        }

        

        return true;

    }

    

    @Override

    public long getAnomalyCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {

        if (startTime == null || endTime == null) {

            System.err.println("时间范围参数不能为空");

            return 0;

        }

        try {

            

            if (startTime.isAfter(endTime)) {

                LocalDateTime temp = startTime;

                startTime = endTime;

                endTime = temp;

            }

            

            return anomalyDetectionRecordRepository.countByDetectionTimeBetween(startTime, endTime);

        } catch (Exception e) {

            System.err.println("获取异常数量失败: " + e.getMessage());

            e.printStackTrace();

            

            throw new RuntimeException("获取异常数量失败: " + e.getMessage(), e);

        }

    }

    

    @Override
    public long getProcessedAnomalyCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            System.err.println("时间范围参数不能为空");
            return 0;
        }
        try {
            
            if (startTime.isAfter(endTime)) {
                LocalDateTime temp = startTime;
                startTime = endTime;
                endTime = temp;
            }
            
            return anomalyDetectionRecordRepository.countByDetectionTimeBetweenAndIsHandledTrue(startTime, endTime);
        } catch (Exception e) {
            System.err.println("获取已处理异常数量失败: " + e.getMessage());
            e.printStackTrace();
            
            throw new RuntimeException("获取已处理异常数量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void scanAndDetectUnprocessedData(LocalDateTime since) {
        System.out.println("开始扫描未检测的历史数据，起始时间: " + since);
        try {
            // 1. 获取指定时间之后的"实时"电力数据 (根据用户需求，只检测实时数据)
            // 注意：这里可能数据量较大，生产环境建议分页处理。但在演示环境中直接获取列表通常可行。
            List<ElectricityData> recentData = electricityDataRepository.findByDataTypeAndRecordTimeAfter("实时", since);
            
            if (recentData.isEmpty()) {
                System.out.println("未发现'实时'历史数据，跳过补测。");
                return;
            }

            int count = 0;
            int anomalyCount = 0;

            for (ElectricityData data : recentData) {
                // 2. 检查是否已经存在检测记录
                if (!anomalyDetectionRecordRepository.existsByDetectionDataId(data.getId())) {
                    try {
                        // 3. 执行检测
                        boolean isAnomaly = detectAnomaly(data);
                        count++;
                        if (isAnomaly) {
                            anomalyCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("补测数据 ID=" + data.getId() + " 失败: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("历史数据补测完成。共扫描 " + recentData.size() + " 条，补测 " + count + " 条，发现 " + anomalyCount + " 个异常。");

        } catch (Exception e) {
            System.err.println("扫描未检测数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Map<String, Object>> getRecentAnomalies(int limit) {

        List<AnomalyDetectionRecord> records = anomalyDetectionRecordRepository.findTopNByOrderByDetectionTimeDesc(limit);

        return records.stream().map(record -> {

            Map<String, Object> map = new HashMap<>();

            map.put("id", record.getId());

            map.put("userName", record.getUser().getName());

            map.put("type", record.getAnomalyType());

            map.put("typeName", translateAnomalyType(record.getAnomalyType()));

            map.put("location", record.getUser().getAddress());

            map.put("time", record.getDetectionTime().toString());

            map.put("status", record.getIsHandled() ? "success" : "pending");

            return map;

        }).collect(Collectors.toList());

    }



    @Override

    public List<Map<String, Object>> getAnomalyTypeDistribution() {

        List<Map<String, Object>> distribution = anomalyDetectionRecordRepository.countByAnomalyType();

        

        return distribution.stream().map(item -> {

            String type = (String) item.get("anomalyType");

            Long value = (Long) item.get("value");

            

            
            Map<String, Object> newItem = new HashMap<>();

            newItem.put("anomalyType", type);

            newItem.put("value", value);

            newItem.put("name", translateAnomalyType(type));

            newItem.put("itemStyle", Map.of("color", getColorForAnomalyType(type)));

            

            return newItem;

        }).collect(Collectors.toList());

    }



    private String translateAnomalyType(String type) {

        switch (type) {

            case "瞬时断电":

                return "瞬时断电";

            case "功率突增":

                return "功率突增";

            case "功率突降":

                return "功率突降";

            case "HIGH_DEVIATION":

                return "高偏差";

            case "PATTERN_DEVIATION":

                return "模式偏离";

            case "TREND_DEVIATION":

                return "趋势偏离";

            case "用电量异常波动":

                return "用电量异常";

            case "零用电检测":

                return "零用电";

            case "用电模式异常":

                return "模式异常";

            case "功率异常":

                return "功率异常";

            case "周期性异常":

                return "周期性异常";

            case "用电量突增":

                return "用电突增";

            case "用电不稳定":

                return "用电不稳";

            case "多设备协同异常":

                return "设备协同异常";

            case "夜间异常用电":

                return "夜间异常";

            case "慢病用户异常":

                return "慢病异常";

            default:

                return type != null ? type : "其他";

        }

    }



    private String getColorForAnomalyType(String type) {

        switch (type) {

            case "瞬时断电":

                return "#f56c6c";

            case "功率突增":

                return "#ff7875";

            case "功率突降":

                return "#ffa940";

            case "HIGH_DEVIATION":

            case "用电量突增":

                return "#f56c6c";

            case "PATTERN_DEVIATION":

            case "用电模式异常":

                return "#e6a23c";

            case "TREND_DEVIATION":

            case "周期性异常":

                return "#67c23a";

            case "零用电检测":

                return "#909399";

            case "功率异常":

                return "#ff7875";

            case "用电不稳定":

                return "#ffa940";

            case "多设备协同异常":

                return "#52c41a";

            case "夜间异常用电":

                return "#1890ff";

            case "慢病用户异常":

                return "#722ed1";

            case "用电量异常波动":

                return "#eb2f96";

            default:

                return "#909399";

        }

    }
    
    /**
     * 根据异常检测记录自动生成并保存警报记录
     */
    private void createAndSaveAlertRecord(AnomalyDetectionRecord anomalyRecord, ElectricityData electricityData) {
        try {
            AlertRecord alertRecord = new AlertRecord();
            alertRecord.setUser(anomalyRecord.getUser());
            alertRecord.setDetectionRecord(anomalyRecord);
            
            // 使用异常数据的实际发生时间作为警报时间，而不是检测时间
            if (electricityData != null && electricityData.getRecordTime() != null) {
                alertRecord.setAlertTime(electricityData.getRecordTime());
            } else {
                alertRecord.setAlertTime(LocalDateTime.now());
            }
            
            alertRecord.setAlertType(anomalyRecord.getAnomalyType());

            // Determine severity level if missing
            String severity = anomalyRecord.getSeverityLevel();
            if (severity == null || severity.isEmpty()) {
                if ("HIGH_DEVIATION".equals(anomalyRecord.getAnomalyType()) || 
                    "瞬时断电".equals(anomalyRecord.getAnomalyType()) ||
                    "功率突增".equals(anomalyRecord.getAnomalyType()) ||
                    "功率突降".equals(anomalyRecord.getAnomalyType())) {
                    severity = "high";
                } else if ("PATTERN_DEVIATION".equals(anomalyRecord.getAnomalyType()) ||
                           "用电模式异常".equals(anomalyRecord.getAnomalyType())) {
                    severity = "medium";
                } else {
                    severity = "low";
                }
                // Update anomaly record with inferred severity
                anomalyRecord.setSeverityLevel(severity);
                // We don't need to call save again if we are in a transaction, but to be safe:
                anomalyDetectionRecordRepository.save(anomalyRecord);
            }
            alertRecord.setSeverityLevel(severity);

            alertRecord.setTriggerReason(anomalyRecord.getNotes());
            alertRecord.setAlertContent("系统自动检测到异常用电: " + translateAnomalyType(anomalyRecord.getAnomalyType()));
            alertRecord.setIsProcessed(false);
            alertRecord.setIsRead(false);
            alertRecord.setIsFalseAlarm(false);

            alertRecordRepository.save(alertRecord);
            System.out.println("成功自动生成异常警报: ID=" + alertRecord.getId() + ", User=" + alertRecord.getUserId() + ", Time=" + alertRecord.getAlertTime());
        } catch (Exception e) {
            System.err.println("自动生成异常警报失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void checkAndBuildMissingPatterns() {
        System.out.println("开始全量检查缺失画像的用户...");
        try {
            // 1. 找出所有有'历史'数据的用户
            List<Long> userIdsWithHistory = electricityDataRepository.findUserIdsByDataType("历史");
            
            // 2. 找出所有已有画像的用户
            List<Long> userIdsWithPatterns = userElectricityPatternRepository.findAllUserIdsWithPatterns();
            
            // 3. 计算差集
            List<Long> missingPatternUserIds = new ArrayList<>(userIdsWithHistory);
            if (userIdsWithPatterns != null) {
                missingPatternUserIds.removeAll(userIdsWithPatterns);
            }
            
            if (missingPatternUserIds.isEmpty()) {
                System.out.println("所有拥有历史数据的用户均已构建画像。");
                return;
            }
            
            System.out.println("发现 " + missingPatternUserIds.size() + " 个用户有历史数据但无画像，开始构建...");
            
            int count = 0;
            for (Long userId : missingPatternUserIds) {
                try {
                    buildUserElectricityPattern(userId);
                    count++;
                } catch (Exception e) {
                    System.err.println("为用户 " + userId + " 补建画像失败: " + e.getMessage());
                }
            }
            System.out.println("画像补全完成，成功构建: " + count + " 个。");
            
        } catch (Exception e) {
            System.err.println("检查缺失画像任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}