package com.powermanage.service.impl;



import com.powermanage.entity.ElectricityData;

import com.powermanage.entity.User;

import com.powermanage.entity.UserElectricityPattern;

import com.powermanage.repository.ElectricityDataRepository;

import com.powermanage.repository.UserElectricityPatternRepository;
import com.powermanage.repository.UserRepository;

import com.powermanage.service.ElectricityPatternAnalysisService;

import com.powermanage.util.AlgorithmUtil;

import com.powermanage.util.DataPreprocessingUtil;

import org.json.JSONArray;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.*;

import java.util.stream.Collectors;





@Service

public class ElectricityPatternAnalysisServiceImpl implements ElectricityPatternAnalysisService {



    @Autowired

    private DataPreprocessingUtil dataPreprocessingUtil;



    @Autowired

    private AlgorithmUtil algorithmUtil;



    @Autowired

    private ElectricityDataRepository electricityDataRepository;



    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserElectricityPatternRepository userElectricityPatternRepository;

    private static final int DEFAULT_ANALYSIS_DAYS = 365; 

    private static final int DEFAULT_INTERVAL_MINUTES = 15; 

    private static final int DEFAULT_WINDOW_SIZE = 5; 



    @Override
    public UserElectricityPattern generateAndSaveUserPattern(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(DEFAULT_ANALYSIS_DAYS);
        List<ElectricityData> historicalData = electricityDataRepository.findByUser_IdAndRecordTimeBetween(userId, startDate, endDate);
        
        if (historicalData == null || historicalData.isEmpty()) {
            // Check if there is ANY data if 365 days is empty
            historicalData = electricityDataRepository.findByUser_IdAndRecordTimeBetween(userId, endDate.minusYears(10), endDate);
            if (historicalData == null || historicalData.isEmpty()) {
                 // Create a dummy pattern or throw exception? 
                 // Better to return null or throw exception.
                 // For now, let's create an empty pattern to avoid errors but mark it.
                 UserElectricityPattern emptyPattern = new UserElectricityPattern();
                 emptyPattern.setUser(user);
                 emptyPattern.setPatternType("无数据");
                 emptyPattern.setAnalysisDate(LocalDateTime.now());
                 emptyPattern.setConfidence(0.0);
                 emptyPattern.setFeatureVector("{}");
                 emptyPattern.setPatternData("{}");
                 return userElectricityPatternRepository.save(emptyPattern);
            }
        }
        
        List<ElectricityData> preprocessedData = dataPreprocessingUtil.preprocessData(historicalData, DEFAULT_INTERVAL_MINUTES, DEFAULT_WINDOW_SIZE);
        
        // If preprocessing filters out everything
        if (preprocessedData.isEmpty()) {
             preprocessedData = historicalData; // Fallback to raw data if needed or just handle empty
        }

        UserElectricityPattern pattern = buildUserPattern(user, preprocessedData);
        List<String> tags = generatePatternTags(user, pattern);
        pattern.setFeatureTags(tags);
        
        Optional<UserElectricityPattern> existingPatternOpt = userElectricityPatternRepository.findByUser_Id(userId);
        if (existingPatternOpt.isPresent()) {
            UserElectricityPattern existing = existingPatternOpt.get();
            existing.setPatternData(pattern.getPatternData());
            existing.setFeatureVector(pattern.getFeatureVector());
            existing.setFeatureTags(pattern.getFeatureTags());
            existing.setAnalysisDate(LocalDateTime.now());
            existing.setPatternType(pattern.getPatternType());
            existing.setConfidence(pattern.getConfidence());
            existing.setDataPeriod(pattern.getDataPeriod());
            existing.setVersion(pattern.getVersion());
            return userElectricityPatternRepository.save(existing);
        } else {
            return userElectricityPatternRepository.save(pattern);
        }
    }

    @Override
    public UserElectricityPattern getUserPattern(Long userId) {
        return userElectricityPatternRepository.findByUser_Id(userId).orElse(null);
    }

    @Override
    public ElectricityPatternAnalysisResult analyzeUserPattern(User user, List<ElectricityData> historicalData) {

        ElectricityPatternAnalysisResult result = new ElectricityPatternAnalysisResult();

        

        try {

            if (historicalData == null || historicalData.isEmpty()) {

                result.setAnalysisNotes("历史数据不足，无法进行用电规律分析");

                return result;

            }



            

            List<ElectricityData> preprocessedData = dataPreprocessingUtil.preprocessData(

                    historicalData, DEFAULT_INTERVAL_MINUTES, DEFAULT_WINDOW_SIZE);



            if (preprocessedData.isEmpty()) {

                result.setAnalysisNotes("预处理后数据为空，无法进行用电规律分析");

                return result;

            }



            

            UserElectricityPattern pattern = buildUserPattern(user, preprocessedData);

            result.setPattern(pattern);



            

            List<String> patternTags = generatePatternTags(user, pattern);

            result.setPatternTags(patternTags);



            

            Map<String, Object> statistics = calculateStatistics(preprocessedData);

            result.setStatistics(statistics);



            

            Map<String, Object> dailyPattern = analyzeDailyPattern(preprocessedData);

            result.setDailyPattern(dailyPattern);



            

            Map<String, Object> weeklyPattern = analyzeWeeklyPattern(preprocessedData);

            result.setWeeklyPattern(weeklyPattern);



            

            Map<String, Object> monthlyPattern = analyzeMonthlyPattern(preprocessedData);

            result.setMonthlyPattern(monthlyPattern);



            

            String analysisNotes = generateAnalysisNotes(user, pattern, patternTags, statistics);

            result.setAnalysisNotes(analysisNotes);



        } catch (Exception e) {

            result.setAnalysisNotes("用电规律分析过程中出现错误: " + e.getMessage());

        }



        return result;

    }



    @Override

    public UserElectricityPattern buildUserPattern(User user, List<ElectricityData> historicalData) {

        UserElectricityPattern pattern = new UserElectricityPattern();

        pattern.setUser(user);

        pattern.setAnalysisDate(LocalDateTime.now());

        pattern.setDataPeriod("1年");



        try {

            

            double avgPower = historicalData.stream()

                    .mapToDouble(ElectricityData::getPower)

                    .average().orElse(0.0);

            

            double powerVariance = historicalData.stream()

                    .mapToDouble(d -> Math.pow(d.getPower() - avgPower, 2))

                    .average().orElse(0.0);

            double powerStdDev = Math.sqrt(powerVariance);

            

            double maxPower = historicalData.stream()

                    .mapToDouble(ElectricityData::getPower)

                    .max().orElse(0.0);

            

            double minPower = historicalData.stream()

                    .mapToDouble(ElectricityData::getPower)

                    .min().orElse(0.0);



            double avgConsumption = historicalData.stream()

                    .mapToDouble(ElectricityData::getElectricityConsumption)

                    .average().orElse(0.0);



            

            JSONObject featureVector = new JSONObject();

            featureVector.put("avgPower", avgPower);

            featureVector.put("stdDev", powerStdDev);

            featureVector.put("maxPower", maxPower);

            featureVector.put("minPower", minPower);

            featureVector.put("avgConsumption", avgConsumption);

            featureVector.put("powerRange", maxPower - minPower);

            featureVector.put("coefficientOfVariation", powerStdDev / avgPower);



            pattern.setFeatureVector(featureVector.toString());



            

            JSONObject patternData = buildPatternData(historicalData);

            pattern.setPatternData(patternData.toString());



            

            String patternType = determinePatternType(user, avgPower, powerStdDev, avgConsumption);

            pattern.setPatternType(patternType);



            

            double confidence = calculatePatternConfidence(historicalData);

            pattern.setConfidence(confidence);



        } catch (Exception e) {

            

            pattern.setFeatureVector("{}");

            pattern.setPatternData("{}");

            pattern.setPatternType("未知");

            pattern.setConfidence(0.0);

        }



        return pattern;

    }



    @Override

    public UserElectricityPattern updateUserPattern(Long userId, List<ElectricityData> newData) {

        

        

        User user = new User();

        user.setId(userId);

        return buildUserPattern(user, newData);

    }



    @Override

    public List<String> generatePatternTags(User user, UserElectricityPattern pattern) {

        List<String> tags = new ArrayList<>();



        try {

            JSONObject featureVector = new JSONObject(pattern.getFeatureVector());

            double avgPower = featureVector.optDouble("avgPower", 0.0);

            double stdDev = featureVector.optDouble("stdDev", 0.0);

            double maxPower = featureVector.optDouble("maxPower", 0.0);

            double minPower = featureVector.optDouble("minPower", 0.0);



            

            if (user.getIsSingleAlone() != null && user.getIsSingleAlone()) {

                tags.add("单人独居老人");

            }

            if (user.getIsCoupleAlone() != null && user.getIsCoupleAlone()) {

                tags.add("双人独居老人");

            }


            if (user.getIsMobilityImpaired() != null && user.getIsMobilityImpaired()) {

                tags.add("行动不便老人");

            }

            if (user.getIsRelianceEquipment() != null && user.getIsRelianceEquipment()) {

                tags.add("依赖设备用户");

            }

            if (user.getIsChronicDisease() != null && user.getIsChronicDisease()) {

                tags.add("康复/慢病患者");

            }

            if (user.getIsRelianceEnvironment() != null && user.getIsRelianceEnvironment()) {

                tags.add("依赖环境用户");

            }

            if (user.getIsProduction() != null && user.getIsProduction()) {

                tags.add("生产用户");

            }



            

            if (avgPower < 100) {

                tags.add("低功耗用户");

            } else if (avgPower > 500) {

                tags.add("高功耗用户");

            } else {

                tags.add("中等功耗用户");

            }



            

            double coefficientOfVariation = stdDev / avgPower;

            if (coefficientOfVariation < 0.2) {

                tags.add("用电稳定");

            } else if (coefficientOfVariation > 0.5) {

                tags.add("用电波动大");

            } else {

                tags.add("用电适中");

            }



            

            if (hasNightUsage(pattern)) {

                tags.add("夜间用电");

            }



            

            if (hasRegularDeviceUsage(pattern)) {

                tags.add("规律设备使用");

            }



        } catch (Exception e) {

            tags.add("模式分析异常");

        }



        return tags;

    }



    @Override

    public Map<String, Object> predictElectricityUsage(Long userId, int period) {

        Map<String, Object> prediction = new HashMap<>();

        

        try {

            
            LocalDateTime endTime = LocalDateTime.now();

            LocalDateTime startTime = endTime.minusDays(DEFAULT_ANALYSIS_DAYS);

            List<ElectricityData> historicalData = electricityDataRepository.findByUser_IdAndRecordTimeBetween(userId, startTime, endTime);

            

            if (historicalData == null || historicalData.isEmpty()) {

                
                prediction.put("userId", userId);

                prediction.put("period", period);

                prediction.put("predictedDailyUsage", 0.0);

                prediction.put("predictedTotalUsage", 0.0);

                prediction.put("confidence", 0.0);

                prediction.put("predictionDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                prediction.put("message", "该用户暂无历史用电数据，无法进行预测");

                return prediction;

            }

            

            
            Map<String, Double> dailyUsage = historicalData.stream()

                .collect(Collectors.groupingBy(

                    data -> data.getRecordTime().toLocalDate().toString(),

                    Collectors.summingDouble(ElectricityData::getElectricityConsumption)

                ));

            

            
            double averageDailyUsage = dailyUsage.values().stream()

                .mapToDouble(Double::doubleValue)

                .average()

                .orElse(0.0);

            

            
            double variance = dailyUsage.values().stream()

                .mapToDouble(usage -> Math.pow(usage - averageDailyUsage, 2))

                .average()

                .orElse(0.0);

            double stdDev = Math.sqrt(variance);

            

            
            double coefficientOfVariation = averageDailyUsage > 0 ? stdDev / averageDailyUsage : 1.0;

            

            
            double confidence = 0.5; 
            if (dailyUsage.size() >= 30) {

                confidence += 0.2; 
            }

            if (dailyUsage.size() >= 90) {

                confidence += 0.2; 
            }

            if (coefficientOfVariation < 0.2) {

                confidence += 0.1; 
            }

            

            
            confidence = Math.min(0.95, Math.max(0.0, confidence));

            

            
            double trendFactor = 1.0; 
            List<String> sortedDates = dailyUsage.keySet().stream()

                .sorted()

                .collect(Collectors.toList());

                

            if (sortedDates.size() >= 14) {

                
                double recentAvg = sortedDates.subList(sortedDates.size() - 7, sortedDates.size()).stream()

                    .mapToDouble(date -> dailyUsage.get(date))

                    .average()

                    .orElse(averageDailyUsage);

                    

                
                double previousAvg = sortedDates.subList(sortedDates.size() - 14, sortedDates.size() - 7).stream()

                    .mapToDouble(date -> dailyUsage.get(date))

                    .average()

                    .orElse(averageDailyUsage);

                    

                
                trendFactor = 1.0 + (recentAvg - previousAvg) / averageDailyUsage * 0.3; 
                trendFactor = Math.max(0.9, Math.min(1.1, trendFactor)); 
            }

            

            
            int currentMonth = LocalDateTime.now().getMonthValue();

            double seasonalFactor = 1.0;

            
            if (currentMonth >= 6 && currentMonth <= 8) {

                seasonalFactor = 1.05; 
            } else if (currentMonth >= 12 || currentMonth <= 2) {

                seasonalFactor = 1.08; 
            } else {

                seasonalFactor = 0.95; 
            }

            

            
            Map<Integer, Double> weeklyPattern = new HashMap<>();

            for (Map.Entry<String, Double> entry : dailyUsage.entrySet()) {

                try {

                    int dayOfWeek = LocalDateTime.parse(entry.getKey() + "T00:00:00").getDayOfWeek().getValue();

                    weeklyPattern.put(dayOfWeek, weeklyPattern.getOrDefault(dayOfWeek, 0.0) + entry.getValue());

                } catch (Exception e) {

                    
                }

            }

            

            
            Map<Integer, Double> weeklyAvgPattern = new HashMap<>();

            for (Map.Entry<Integer, Double> entry : weeklyPattern.entrySet()) {

                
                long count = dailyUsage.keySet().stream()

                    .filter(date -> {

                        try {

                            return LocalDateTime.parse(date + "T00:00:00").getDayOfWeek().getValue() == entry.getKey();

                        } catch (Exception e) {

                            return false;

                        }

                    })

                    .count();

                    

                if (count > 0) {

                    weeklyAvgPattern.put(entry.getKey(), entry.getValue() / count);

                }

            }

            

            
            double predictedDailyUsage = averageDailyUsage * trendFactor * seasonalFactor;

            

            
            double predictedTotalUsage = predictedDailyUsage * period;

            

            
            if (userRepository == null) {

                prediction.put("warning", "用户仓库未初始化，跳过用户类型调整");

            } else {

                try {

                    Optional<User> userOpt = userRepository.findById(userId);

                    if (userOpt.isPresent()) {

                        User user = userOpt.get();

                         if (user.getIsRelianceEquipment() != null && user.getIsRelianceEquipment()) {

                            predictedDailyUsage *= 1.15; 
                            confidence += 0.05; 
                        }

                        if (user.getIsRelianceEnvironment() != null && user.getIsRelianceEnvironment()) {

                            predictedDailyUsage *= 1.20; 
                            confidence += 0.05; 
                        }

                        if (user.getIsProduction() != null && user.getIsProduction()) {

                            predictedDailyUsage *= 1.18; 
                            confidence += 0.05; 
                        }

                        if (user.getIsSingleAlone() != null && user.getIsSingleAlone()) {

                            predictedDailyUsage *= 0.85; 
                        }

                        

                        
                        predictedTotalUsage = predictedDailyUsage * period;

                    } else {

                        prediction.put("warning", "未找到用户ID为 " + userId + " 的用户信息");

                    }

                } catch (Exception e) {

                    prediction.put("warning", "获取用户信息时出错: " + e.getMessage());

                    e.printStackTrace();

                }

            }

            

            
            confidence = Math.min(0.95, Math.max(0.0, confidence));

            

            prediction.put("userId", userId);

            prediction.put("period", period);

            prediction.put("predictedDailyUsage", Math.round(predictedDailyUsage * 100.0) / 100.0);

            prediction.put("predictedTotalUsage", Math.round(predictedTotalUsage * 100.0) / 100.0);

            prediction.put("confidence", Math.round(confidence * 100.0) / 100.0);

            prediction.put("dataPoints", dailyUsage.size());

            prediction.put("coefficientOfVariation", Math.round(coefficientOfVariation * 100.0) / 100.0);

            prediction.put("trendFactor", Math.round(trendFactor * 100.0) / 100.0);

            prediction.put("seasonalFactor", Math.round(seasonalFactor * 100.0) / 100.0);

            prediction.put("weeklyPattern", weeklyAvgPattern);

            prediction.put("predictionDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            

        } catch (Exception e) {

            e.printStackTrace();

            prediction.put("error", "预测过程中出现错误: " + e.getMessage());

        }

        

        return prediction;

    }



    @Override
    public Map<String, Object> analyzePatternTrend(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> trend = new HashMap<>();
        
        try {
            trend.put("userId", userId);
            trend.put("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            trend.put("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            trend.put("analysisDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            List<ElectricityData> dataList = electricityDataRepository.findByUser_IdAndRecordTimeBetween(userId, startDate, endDate);
            
            if (dataList == null || dataList.isEmpty()) {
                trend.put("trendDirection", "无数据");
                trend.put("changeRate", 0.0);
                trend.put("message", "指定时间段内无数据");
                return trend;
            }

            // 按天分组计算日均用电量
            Map<String, Double> dailyUsage = dataList.stream()
                .collect(Collectors.groupingBy(
                    data -> data.getRecordTime().toLocalDate().toString(),
                    Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                ));
            
            List<String> sortedDates = dailyUsage.keySet().stream().sorted().collect(Collectors.toList());
            
            if (sortedDates.size() < 2) {
                trend.put("trendDirection", "数据不足");
                trend.put("changeRate", 0.0);
                return trend;
            }

            // 将数据分为前后两半进行对比
            int halfSize = sortedDates.size() / 2;
            double firstHalfSum = 0;
            double secondHalfSum = 0;
            
            for (int i = 0; i < halfSize; i++) {
                firstHalfSum += dailyUsage.get(sortedDates.get(i));
            }
            
            for (int i = halfSize; i < sortedDates.size(); i++) {
                secondHalfSum += dailyUsage.get(sortedDates.get(i));
            }
            
            double firstHalfAvg = firstHalfSum / halfSize;
            double secondHalfAvg = secondHalfSum / (sortedDates.size() - halfSize);
            
            double changeRate = 0.0;
            if (firstHalfAvg > 0) {
                changeRate = (secondHalfAvg - firstHalfAvg) / firstHalfAvg;
            }
            
            String direction;
            if (Math.abs(changeRate) < 0.05) {
                direction = "稳定";
            } else if (changeRate > 0) {
                direction = "上升";
            } else {
                direction = "下降";
            }
            
            trend.put("trendDirection", direction);
            trend.put("changeRate", Math.round(changeRate * 10000.0) / 100.0 + "%"); // 转为百分比保留2位小数
            trend.put("firstHalfAvg", Math.round(firstHalfAvg * 100.0) / 100.0);
            trend.put("secondHalfAvg", Math.round(secondHalfAvg * 100.0) / 100.0);
            
        } catch (Exception e) {
            trend.put("error", "趋势分析过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return trend;
    }



    

    private JSONObject buildPatternData(List<ElectricityData> historicalData) {

        JSONObject patternData = new JSONObject();

        

        try {

            

            Map<Integer, Double> hourlyAvgPower = historicalData.stream()

                    .collect(Collectors.groupingBy(

                            data -> data.getRecordTime().getHour(),

                            Collectors.averagingDouble(ElectricityData::getPower)

                    ));



            

            Map<Integer, Double> weeklyAvgPower = historicalData.stream()

                    .collect(Collectors.groupingBy(

                            data -> data.getRecordTime().getDayOfWeek().getValue(),

                            Collectors.averagingDouble(ElectricityData::getPower)

                    ));



            

            List<ElectricityData> recentData = historicalData.stream()

                    .sorted((a, b) -> b.getRecordTime().compareTo(a.getRecordTime()))

                    .limit(96) 

                    .collect(Collectors.toList());



            List<Double> timeSeries = recentData.stream()

                    .map(ElectricityData::getPower)

                    .collect(Collectors.toList());



            patternData.put("hourlyPattern", new JSONObject(hourlyAvgPower));

            patternData.put("weeklyPattern", new JSONObject(weeklyAvgPower));

            patternData.put("timeSeries", new JSONArray(timeSeries));

            patternData.put("dataPoints", historicalData.size());

            patternData.put("analysisDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));



        } catch (Exception e) {

            patternData.put("error", "构建模式数据时出现错误: " + e.getMessage());

        }



        return patternData;

    }



    

    private String determinePatternType(User user, double avgPower, double stdDev, double avgConsumption) {

        

        if (user.getIsRelianceEquipment() != null && user.getIsRelianceEquipment()) {

            return "依赖设备用户";

        } else if (user.getIsChronicDisease() != null && user.getIsChronicDisease()) {

            return "慢病/康复用户";

        } else if (user.getIsRelianceEnvironment() != null && user.getIsRelianceEnvironment()) {

            return "依赖环境用户";

        } else if (user.getIsSingleAlone() != null && user.getIsSingleAlone()) {

            return "单人独居用户";

        } else if (user.getIsCoupleAlone() != null && user.getIsCoupleAlone()) {

            return "双人独居用户";
        
        } else if (user.getIsMobilityImpaired() != null && user.getIsMobilityImpaired()) {

            return "行动不便用户";

        } else if (avgPower < 100) {

            return "低功耗住宅";

        } else if (avgPower > 500) {

            return "高功耗住宅";

        } else {

            return "标准住宅";

        }

    }



    

    private double calculatePatternConfidence(List<ElectricityData> historicalData) {

        if (historicalData.size() < 100) {

            return 0.5; 

        } else if (historicalData.size() < 1000) {

            return 0.7; 

        } else {

            return 0.9; 

        }

    }



    

    private Map<String, Object> calculateStatistics(List<ElectricityData> data) {

        Map<String, Object> stats = new HashMap<>();

        

        double avgPower = data.stream().mapToDouble(ElectricityData::getPower).average().orElse(0.0);

        double avgConsumption = data.stream().mapToDouble(ElectricityData::getElectricityConsumption).average().orElse(0.0);

        double maxPower = data.stream().mapToDouble(ElectricityData::getPower).max().orElse(0.0);

        double minPower = data.stream().mapToDouble(ElectricityData::getPower).min().orElse(0.0);

        

        stats.put("avgPower", avgPower);

        stats.put("avgConsumption", avgConsumption);

        stats.put("maxPower", maxPower);

        stats.put("minPower", minPower);

        stats.put("powerRange", maxPower - minPower);

        stats.put("dataPoints", data.size());

        

        return stats;

    }



    

    private Map<String, Object> analyzeDailyPattern(List<ElectricityData> data) {

        Map<String, Object> dailyPattern = new HashMap<>();

        

        

        Map<Integer, Double> hourlyPower = data.stream()

                .collect(Collectors.groupingBy(

                        d -> d.getRecordTime().getHour(),

                        Collectors.averagingDouble(ElectricityData::getPower)

                ));

        

        dailyPattern.put("hourlyPower", hourlyPower);

        

        

        int peakHour = hourlyPower.entrySet().stream()

                .max(Map.Entry.comparingByValue())

                .map(Map.Entry::getKey)

                .orElse(0);

        

        dailyPattern.put("peakHour", peakHour);

        dailyPattern.put("peakPower", hourlyPower.getOrDefault(peakHour, 0.0));

        

        return dailyPattern;

    }



    

    private Map<String, Object> analyzeWeeklyPattern(List<ElectricityData> data) {

        Map<String, Object> weeklyPattern = new HashMap<>();

        

        

        Map<Integer, Double> weeklyPower = data.stream()

                .collect(Collectors.groupingBy(

                        d -> d.getRecordTime().getDayOfWeek().getValue(),

                        Collectors.averagingDouble(ElectricityData::getPower)

                ));

        

        weeklyPattern.put("weeklyPower", weeklyPower);

        

        return weeklyPattern;

    }



    

    private Map<String, Object> analyzeMonthlyPattern(List<ElectricityData> data) {

        Map<String, Object> monthlyPattern = new HashMap<>();

        

        

        Map<Integer, Double> monthlyPower = data.stream()

                .collect(Collectors.groupingBy(

                        d -> d.getRecordTime().getMonthValue(),

                        Collectors.averagingDouble(ElectricityData::getPower)

                ));

        

        monthlyPattern.put("monthlyPower", monthlyPower);

        

        return monthlyPattern;

    }



    

    private boolean hasNightUsage(UserElectricityPattern pattern) {

        try {

            JSONObject patternData = new JSONObject(pattern.getPatternData());

            if (patternData.has("hourlyPattern")) {

                JSONObject hourlyPattern = patternData.getJSONObject("hourlyPattern");

                

                double nightUsage = 0.0;

                for (int hour = 22; hour <= 23; hour++) {

                    nightUsage += hourlyPattern.optDouble(String.valueOf(hour), 0.0);

                }

                for (int hour = 0; hour <= 6; hour++) {

                    nightUsage += hourlyPattern.optDouble(String.valueOf(hour), 0.0);

                }

                return nightUsage > 0.0;

            }

        } catch (Exception e) {

            

        }

        return false;

    }



    

    private boolean hasRegularDeviceUsage(UserElectricityPattern pattern) {

        try {

            JSONObject featureVector = new JSONObject(pattern.getFeatureVector());

            double coefficientOfVariation = featureVector.optDouble("coefficientOfVariation", 0.0);

            

            return coefficientOfVariation < 0.3;

        } catch (Exception e) {

            return false;

        }

    }



    

    private String generateAnalysisNotes(User user, UserElectricityPattern pattern, 

                                       List<String> tags, Map<String, Object> statistics) {

        StringBuilder notes = new StringBuilder();

        

        notes.append("用户用电规律分析完成。");

        notes.append("平均功率: ").append(String.format("%.2f", statistics.get("avgPower"))).append("W，");

        notes.append("平均用电量: ").append(String.format("%.2f", statistics.get("avgConsumption"))).append("kWh。");

        

        if (!tags.isEmpty()) {

            notes.append("特征标签: ").append(String.join("、", tags)).append("。");

        }

        

        notes.append("模式类型: ").append(pattern.getPatternType()).append("，");

        notes.append("置信度: ").append(String.format("%.1f%%", pattern.getConfidence() * 100));

        

        return notes.toString();

    }

}

