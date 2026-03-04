package com.powermanage.util;

import com.powermanage.config.AnomalyDetectionConfig;
import com.powermanage.entity.ElectricityData;
import com.powermanage.entity.UserElectricityPattern;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class AnomalyDetectionUtil {

    @Autowired
    private DataPreprocessingUtil dataPreprocessingUtil;

    @Autowired
    private AlgorithmUtil algorithmUtil;

    @Autowired
    private AnomalyDetectionConfig config;

    
    public AnomalyDetectionResult detectAnomaly(List<ElectricityData> recentData, UserElectricityPattern pattern) {
        AnomalyDetectionResult result = new AnomalyDetectionResult();
        result.setAnomaly(false);
        result.setAnomalyScore(0.0);

        try {
            if (recentData == null || recentData.isEmpty() || pattern == null) {
                result.setNotes("数据不完整，无法进行异常检测");
                return result;
            }

            
            List<ElectricityData> preprocessedData = dataPreprocessingUtil.preprocessData(
                    recentData, 
                    config.getDataPreprocessing().getIntervalMinutes(), 
                    config.getDataPreprocessing().getWindowSize());

            if (preprocessedData.isEmpty()) {
                result.setNotes("预处理后数据为空，无法进行异常检测");
                return result;
            }

            
            double[] recentFeatures = extractFeaturesFromData(preprocessedData);
            double[] patternFeatures = extractFeaturesFromPattern(pattern);

            
            Map<String, Double> algorithmResults = new HashMap<>();
            
            
            double mahalanobisDistance = calculateMahalanobisDistance(recentFeatures, patternFeatures);
            algorithmResults.put("mahalanobis", mahalanobisDistance);
            
            
            double[] recentTimeSeries = extractTimeSeries(preprocessedData);
            double[] patternTimeSeries = extractPatternTimeSeries(pattern, recentTimeSeries.length);
            double dtwDistance = algorithmUtil.calculateConstrainedDTWDistance(
                    recentTimeSeries, patternTimeSeries, config.getDtwParams().getWindowRatio());
            algorithmResults.put("dtw", dtwDistance);
            
            
            double zScore = calculateZScore(preprocessedData, pattern);
            algorithmResults.put("zscore", zScore);

            
            boolean isAnomaly = algorithmUtil.performComprehensiveAnomalyDetection(
                    algorithmResults, config.getAlgorithmThresholds(), config.getAlgorithmWeights());

            
            double comprehensiveScore = calculateComprehensiveScore(algorithmResults);

            
            result.setAnomaly(isAnomaly);
            result.setAnomalyScore(comprehensiveScore);
            result.setAlgorithmResults(algorithmResults);
            
            
            if (isAnomaly) {
                result.setNotes(generateAnomalyNotes(algorithmResults, preprocessedData, pattern));
            } else {
                result.setNotes("用电正常");
            }

        } catch (Exception e) {
            result.setNotes("异常检测过程中出现错误: " + e.getMessage());
            result.setAnomaly(false);
        }

        return result;
    }

    public AnomalyDetectionResult detectWithHistory(List<ElectricityData> recentData, List<ElectricityData> historyData) {
        AnomalyDetectionResult result = new AnomalyDetectionResult();
        result.setAnomaly(false);
        result.setAnomalyScore(0.0);
        try {
            if (recentData == null || recentData.isEmpty() || historyData == null || historyData.isEmpty()) {
                result.setNotes("数据不完整，无法进行异常检测");
                return result;
            }
            int interval = config.getDataPreprocessing().getIntervalMinutes();
            int window = config.getDetection().getFeatureWindowSize();
            List<ElectricityData> preRecent = dataPreprocessingUtil.preprocessData(recentData, interval, config.getDataPreprocessing().getWindowSize());
            List<ElectricityData> preHist = dataPreprocessingUtil.preprocessData(historyData, interval, config.getDataPreprocessing().getWindowSize());
            if (preRecent.isEmpty() || preHist.isEmpty()) {
                result.setNotes("预处理后数据为空，无法进行异常检测");
                return result;
            }
            double[] recentPower = preRecent.stream().sorted(Comparator.comparing(ElectricityData::getRecordTime)).mapToDouble(ElectricityData::getPower).toArray();
            double[] histPower = preHist.stream().sorted(Comparator.comparing(ElectricityData::getRecordTime)).mapToDouble(ElectricityData::getPower).toArray();
            double[][] histFeatures = extractWindowFeatures(histPower, window);
            double[][] newFeatures = extractWindowFeatures(concat(histPower, recentPower), window);
            
            // Initialize algorithm results
            Map<String, Double> algorithmResults = new HashMap<>();
            double histMean = Arrays.stream(histPower).average().orElse(0.0);
            double histStd = stddev(histPower);
            double recentAvg = Arrays.stream(recentPower).average().orElse(0.0);
            double z = algorithmUtil.calculateZScore(recentAvg, histMean, histStd > 0 ? histStd : 1.0);
            algorithmResults.put("zscore", z);
            
            boolean anomalyM = false;
            boolean anomalyD = false;

            if (histFeatures.length > 0 && newFeatures.length > 0) {
                double[] meanVec = meanVector(histFeatures);
                double[][] cov = algorithmUtil.calculateCovarianceMatrix(histFeatures);
                double[][] covInv = algorithmUtil.invertMatrix(cov);
                if (covInv == null) {
                    covInv = new double[cov.length][cov.length];
                    for (int i = 0; i < cov.length; i++) {
                        covInv[i][i] = 1.0;
                    }
                }
                double[] mHist = calculateMahalanobisSeries(histFeatures, meanVec, covInv);
                double alpha = Math.max(1e-6, config.getDetection().getAlphaBase() / Math.max(1, config.getDetection().getSensitivity()));
                double thrM = grubbsThreshold(mHist, alpha);
                double[] mNew = calculateMahalanobisSeries(newFeatures, meanVec, covInv);
                int lastIdx = mNew.length - 1;
                anomalyM = lastIdx >= 0 && mNew[lastIdx] > thrM;
                algorithmResults.put("mahalanobis", mNew.length > 0 ? mNew[lastIdx] : 0.0);

                double[] dtwSeries = nestedDtw(newFeatures, histFeatures, window);
                double dtwMean = average(dtwSeries);
                double dtwStd = stddev(dtwSeries);
                double dtwFactor = config.getDetection().getDtwStdFactorBase() * (config.getDetection().getSensitivity() / 5.0);
                double thrD = dtwMean + dtwFactor * dtwStd;
                anomalyD = dtwSeries.length > 0 && dtwSeries[dtwSeries.length - 1] > thrD;
                algorithmResults.put("dtw", dtwSeries.length > 0 ? dtwSeries[dtwSeries.length - 1] : 0.0);
            } else {
                // Fallback: If not enough points for window features, rely on Z-Score
                algorithmResults.put("mahalanobis", 0.0);
                algorithmResults.put("dtw", 0.0);
            }
            
            // Check Z-Score threshold (fallback or additional check)
            boolean anomalyZ = Math.abs(z) > config.getAlgorithmThresholds().getOrDefault("zscore", 3.0);
            
            boolean isAnomaly = anomalyM || anomalyD || (histFeatures.length == 0 && anomalyZ);
            result.setAnomaly(isAnomaly);
            result.setAlgorithmResults(algorithmResults);
            result.setAnomalyScore(isAnomaly ? 1.0 : 0.0);
            if (isAnomaly) {
                String t = classifyHighLow(recentPower.length > 0 ? recentPower[recentPower.length - 1] : recentAvg, histMean, histStd);
                result.setNotes(t);
            } else {
                result.setNotes("用电正常");
            }
        } catch (Exception e) {
            result.setNotes("异常检测过程中出现错误: " + e.getMessage());
            result.setAnomaly(false);
        }
        return result;
    }

    private double[][] extractWindowFeatures(double[] series, int windowSize) {
        if (series == null || series.length <= windowSize) {
            return new double[0][0];
        }
        int n = series.length;
        double[][] features = new double[n - windowSize][2];
        for (int i = windowSize; i < n; i++) {
            double var = variance(series, i - windowSize, i);
            double curr = series[i];
            features[i - windowSize][0] = var;
            features[i - windowSize][1] = curr;
        }
        return features;
    }

    private double[] calculateMahalanobisSeries(double[][] features, double[] mean, double[][] covInv) {
        double[] d = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            d[i] = algorithmUtil.calculateMahalanobisDistanceWithInverse(features[i], mean, covInv);
        }
        return d;
    }

    private double[] meanVector(double[][] features) {
        if (features == null || features.length == 0) return new double[0];
        int n = features.length;
        int k = features[0].length;
        double[] mean = new double[k];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < k; j++) {
                mean[j] += features[i][j];
            }
        }
        for (int j = 0; j < k; j++) {
            mean[j] /= n;
        }
        return mean;
    }

    private double[] nestedDtw(double[][] newFeatures, double[][] histFeatures, int windowSize) {
        int maxLen = Math.min(newFeatures.length, histFeatures.length);
        int upper = maxLen - windowSize;
        if (upper <= 0) {
            return new double[0];
        }
        double[] dists = new double[upper];
        for (int i = 0; i < upper; i++) {
            double[][] cur = Arrays.copyOfRange(newFeatures, i, i + windowSize);
            double[][] his = Arrays.copyOfRange(histFeatures, i, i + windowSize);
            dists[i] = algorithmUtil.calculateDTWDistanceForVectors(cur, his);
        }
        return dists;
    }

    private double grubbsThreshold(double[] distances, double alpha) {
        if (distances == null || distances.length == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double mean = average(distances);
        double std = stddev(distances);
        if (std == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double z = algorithmUtil.inverseNormalCdf(1.0 - alpha / 2.0);
        return mean + std * z;
    }

    private double average(double[] v) {
        if (v == null || v.length == 0) return 0.0;
        double s = 0.0;
        for (double x : v) s += x;
        return s / v.length;
    }

    private double stddev(double[] v) {
        if (v == null || v.length == 0) return 0.0;
        double m = average(v);
        double s = 0.0;
        for (double x : v) {
            double d = x - m;
            s += d * d;
        }
        return Math.sqrt(s / v.length);
    }

    private double variance(double[] v, int start, int end) {
        int len = end - start;
        if (len <= 0) return 0.0;
        double s = 0.0;
        for (int i = start; i < end; i++) s += v[i];
        double m = s / len;
        double ss = 0.0;
        for (int i = start; i < end; i++) {
            double d = v[i] - m;
            ss += d * d;
        }
        return ss / len;
    }

    private double[] concat(double[] a, double[] b) {
        if (a == null || a.length == 0) return b;
        if (b == null || b.length == 0) return a;
        double[] r = new double[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private String classifyHighLow(double recentValue, double histMean, double histStd) {
        if (histStd <= 0) {
            return "用电模式偏离正常水平";
        }
        double high = histMean + histStd;
        double low = histMean - histStd;
        if (recentValue > high) {
            return "用电功率异常高";
        } else if (recentValue < low) {
            return "用电功率异常低";
        }
        return "用电模式偏离正常水平";
    }

    
    private double[] extractFeaturesFromData(List<ElectricityData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new double[0];
        }

        
        double avgPower = dataList.stream().mapToDouble(ElectricityData::getPower).average().orElse(0);
        double powerVariance = dataList.stream().mapToDouble(d -> Math.pow(d.getPower() - avgPower, 2)).average().orElse(0);
        double powerStdDev = Math.sqrt(powerVariance);
        double maxPower = dataList.stream().mapToDouble(ElectricityData::getPower).max().orElse(0);
        double minPower = dataList.stream().mapToDouble(ElectricityData::getPower).min().orElse(0);

        
        double avgConsumption = dataList.stream().mapToDouble(ElectricityData::getElectricityConsumption).average().orElse(0);
        double consumptionVariance = dataList.stream().mapToDouble(d -> Math.pow(d.getElectricityConsumption() - avgConsumption, 2)).average().orElse(0);
        double consumptionStdDev = Math.sqrt(consumptionVariance);

        
        return new double[] {
            avgPower, powerStdDev, maxPower, minPower, 
            avgConsumption, consumptionStdDev
        };
    }

    
    private double[] extractFeaturesFromPattern(UserElectricityPattern pattern) {
        if (pattern == null || pattern.getFeatureVector() == null) {
            return new double[0];
        }

        try {
            JSONObject featureVector = new JSONObject(pattern.getFeatureVector());
            return new double[] {
                featureVector.optDouble("avgPower", 0.0),
                featureVector.optDouble("stdDev", 0.0),
                featureVector.optDouble("maxPower", 0.0),
                featureVector.optDouble("minPower", 0.0),
                featureVector.optDouble("avgConsumption", 0.0),
                featureVector.optDouble("stdDev", 0.0) 
            };
        } catch (Exception e) {
            return new double[0];
        }
    }

    
    private double[] extractTimeSeries(List<ElectricityData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new double[0];
        }

        
        List<ElectricityData> sortedData = new ArrayList<>(dataList);
        sortedData.sort(Comparator.comparing(ElectricityData::getRecordTime));

        
        return sortedData.stream()
                .mapToDouble(ElectricityData::getPower)
                .toArray();
    }

    
    private double[] extractPatternTimeSeries(UserElectricityPattern pattern, int length) {
        if (pattern == null || pattern.getPatternData() == null) {
            return new double[length];
        }

        try {
            
            JSONObject patternData = new JSONObject(pattern.getPatternData());
            
            
            if (patternData.has("timeSeries")) {
                List<Double> timeSeriesList = patternData.getJSONArray("timeSeries")
                        .toList().stream()
                        .map(obj -> Double.parseDouble(obj.toString()))
                        .collect(Collectors.toList());
                
                
                return adjustSeriesLength(timeSeriesList, length);
            }
            
            
            double avgValue = patternData.optDouble("avgPower", 0.0);
            double[] series = new double[length];
            Arrays.fill(series, avgValue);
            return series;
            
        } catch (Exception e) {
            
            return new double[length];
        }
    }

    
    private double[] adjustSeriesLength(List<Double> series, int targetLength) {
        if (series == null || series.isEmpty()) {
            return new double[targetLength];
        }

        double[] result = new double[targetLength];
        
        if (series.size() == targetLength) {
            
            for (int i = 0; i < series.size(); i++) {
                result[i] = series.get(i);
            }
        } else if (series.size() > targetLength) {
            
            int step = series.size() / targetLength;
            for (int i = 0; i < targetLength; i++) {
                result[i] = series.get(i * step);
            }
        } else {
            
            for (int i = 0; i < series.size(); i++) {
                result[i] = series.get(i);
            }
            for (int i = series.size(); i < targetLength; i++) {
                result[i] = series.get(series.size() - 1);
            }
        }
        
        return result;
    }

    
    private double calculateMahalanobisDistance(double[] recentFeatures, double[] patternFeatures) {
        if (recentFeatures == null || patternFeatures == null || 
            recentFeatures.length != patternFeatures.length || recentFeatures.length < 2) {
            return 0.0;
        }

        try {
            double[] stdDevs = new double[recentFeatures.length];
            // Use pattern standard deviations for normalization
            // Power related metrics scaled by powerStdDev (index 1)
            double powerStd = Math.max(1e-6, patternFeatures[1]);
            stdDevs[0] = powerStd; // avgPower
            stdDevs[1] = powerStd; // stdDev
            stdDevs[2] = powerStd; // maxPower
            stdDevs[3] = powerStd; // minPower
            
            // Consumption related metrics scaled by consumptionStdDev (index 5)
            // Check if patternFeatures has enough length (index 5 exists if length >= 6)
            double consumptionStd = 1.0;
            if (patternFeatures.length > 5) {
                 consumptionStd = Math.max(1e-6, patternFeatures[5]);
            }
            if (stdDevs.length > 4) stdDevs[4] = consumptionStd; // avgConsumption
            if (stdDevs.length > 5) stdDevs[5] = consumptionStd; // consumptionStdDev
            
            return algorithmUtil.calculateSimplifiedMahalanobisDistance(
                    recentFeatures, patternFeatures, stdDevs);
        } catch (Exception e) {
            return 0.0;
        }
    }

    
    private double calculateZScore(List<ElectricityData> dataList, UserElectricityPattern pattern) {
        if (dataList == null || dataList.isEmpty() || pattern == null || pattern.getFeatureVector() == null) {
            return 0.0;
        }

        try {
            JSONObject featureVector = new JSONObject(pattern.getFeatureVector());
            double avgPower = featureVector.optDouble("avgPower", 0.0);
            double stdDev = featureVector.optDouble("stdDev", 1.0);
            
            
            double recentAvgPower = dataList.stream()
                    .mapToDouble(ElectricityData::getPower)
                    .average()
                    .orElse(0);
            
            return algorithmUtil.calculateZScore(recentAvgPower, avgPower, stdDev);
        } catch (Exception e) {
            return 0.0;
        }
    }

    
    private double calculateComprehensiveScore(Map<String, Double> algorithmResults) {
        if (algorithmResults == null || algorithmResults.isEmpty()) {
            return 0.0;
        }

        double weightedScore = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : algorithmResults.entrySet()) {
            String algorithm = entry.getKey();
            double score = entry.getValue();
            double weight = config.getAlgorithmWeights().getOrDefault(algorithm, 1.0);
            double threshold = config.getAlgorithmThresholds().getOrDefault(algorithm, 1.0);
            
            
            double normalizedScore = Math.min(1.0, score / threshold);
            
            weightedScore += normalizedScore * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedScore / totalWeight : 0.0;
    }

    
    private String generateAnomalyNotes(
            Map<String, Double> algorithmResults,
            List<ElectricityData> dataList,
            UserElectricityPattern pattern) {
        StringBuilder notes = new StringBuilder();
        
        try {
            JSONObject featureVector = new JSONObject(pattern.getFeatureVector());
            // Check for missing stdDev in profile
            if (!featureVector.has("stdDev") || featureVector.optDouble("stdDev") == 0) {
                 notes.append("[Warning: Profile stdDev missing/zero] ");
            }

            double avgPower = featureVector.optDouble("avgPower", 0.0);
            double recentAvgPower = dataList.stream()
                    .mapToDouble(ElectricityData::getPower)
                    .average()
                    .orElse(0);
            
            
            if (recentAvgPower < avgPower * 0.1) {
                notes.append("用电功率异常低，可能是设备停止运行");
            } else if (recentAvgPower > avgPower * 2) {
                notes.append("用电功率异常高，可能是设备异常或新增设备");
            } else {
                notes.append("用电模式偏离正常水平");
            }
            
            
            notes.append(" (主要贡献：");
            boolean first = true;
            
            for (Map.Entry<String, Double> entry : algorithmResults.entrySet()) {
                String algorithm = entry.getKey();
                double score = entry.getValue();
                double threshold = config.getAlgorithmThresholds().getOrDefault(algorithm, 1.0);
                
                if (score > threshold) {
                    if (!first) {
                        notes.append(", ");
                    }
                    notes.append(getAlgorithmName(algorithm)).append(": ")
                         .append(String.format("%.2f", score));
                    first = false;
                }
            }
            
            notes.append(")");
            
        } catch (Exception e) {
            notes.append("用电模式偏离正常水平");
        }
        
        return notes.toString();
    }

    
    private String getAlgorithmName(String algorithmCode) {
        Map<String, String> algorithmNames = new HashMap<String, String>() {
            {
                put("mahalanobis", "马氏距离");
                put("dtw", "DTW算法");
                put("zscore", "z-score");
            }
        };
        
        return algorithmNames.getOrDefault(algorithmCode, algorithmCode);
    }

    
    public AnomalyDetectionConfig getConfig() {
        return config;
    }

    
    public static class AnomalyDetectionResult {
        private boolean isAnomaly;
        private double anomalyScore;
        private String notes;
        private Map<String, Double> algorithmResults;
        private LocalDateTime detectionTime;

        public AnomalyDetectionResult() {
            this.detectionTime = LocalDateTime.now();
            this.algorithmResults = new HashMap<>();
        }

        public boolean isAnomaly() { return isAnomaly; }
        public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }
        public double getAnomalyScore() { return anomalyScore; }
        public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public Map<String, Double> getAlgorithmResults() { return algorithmResults; }
        public void setAlgorithmResults(Map<String, Double> algorithmResults) { 
            this.algorithmResults = algorithmResults; 
        }
        public LocalDateTime getDetectionTime() { return detectionTime; }
        public void setDetectionTime(LocalDateTime detectionTime) { 
            this.detectionTime = detectionTime; 
        }
    }
}