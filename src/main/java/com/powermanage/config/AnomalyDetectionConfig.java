package com.powermanage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
@ConfigurationProperties(prefix = "anomaly.detection")
public class AnomalyDetectionConfig {

    
    private Map<String, Double> algorithmWeights = new HashMap<String, Double>() {
        {
            put("mahalanobis", 0.4);    
            put("dtw", 0.3);           
            put("zscore", 0.3);         
        }
    };

    
    private Map<String, Double> algorithmThresholds = new HashMap<String, Double>() {
        {
            put("mahalanobis", 2.5);    
            put("dtw", 10.0);           
            put("zscore", 3.0);         
        }
    };

    
    private DataPreprocessingParams dataPreprocessing = new DataPreprocessingParams();

    
    private DTWParams dtwParams = new DTWParams();

    
    private MahalanobisParams mahalanobisParams = new MahalanobisParams();

    
    private ZScoreParams zScoreParams = new ZScoreParams();

    
    private ComprehensiveParams comprehensiveParams = new ComprehensiveParams();

    private DetectionParams detection = new DetectionParams();

    public DetectionParams getDetection() {
        return detection;
    }

    public void setDetection(DetectionParams detection) {
        this.detection = detection;
    }

    public Map<String, Double> getAlgorithmWeights() {
        return algorithmWeights;
    }

    public void setAlgorithmWeights(Map<String, Double> algorithmWeights) {
        this.algorithmWeights = algorithmWeights;
    }

    public Map<String, Double> getAlgorithmThresholds() {
        return algorithmThresholds;
    }

    public void setAlgorithmThresholds(Map<String, Double> algorithmThresholds) {
        this.algorithmThresholds = algorithmThresholds;
    }

    public DataPreprocessingParams getDataPreprocessing() {
        return dataPreprocessing;
    }

    public void setDataPreprocessing(DataPreprocessingParams dataPreprocessing) {
        this.dataPreprocessing = dataPreprocessing;
    }

    public DTWParams getDtwParams() {
        return dtwParams;
    }

    public void setDtwParams(DTWParams dtwParams) {
        this.dtwParams = dtwParams;
    }

    public MahalanobisParams getMahalanobisParams() {
        return mahalanobisParams;
    }

    public void setMahalanobisParams(MahalanobisParams mahalanobisParams) {
        this.mahalanobisParams = mahalanobisParams;
    }

    public ZScoreParams getZScoreParams() {
        return zScoreParams;
    }

    public void setZScoreParams(ZScoreParams zScoreParams) {
        this.zScoreParams = zScoreParams;
    }

    public ComprehensiveParams getComprehensiveParams() {
        return comprehensiveParams;
    }

    public void setComprehensiveParams(ComprehensiveParams comprehensiveParams) {
        this.comprehensiveParams = comprehensiveParams;
    }

    
    public static class DataPreprocessingParams {
        private int intervalMinutes = 15;           
        private int windowSize = 5;                 
        private double outlierThreshold = 3.0;      
        private int minDataPoints = 10;             
        private boolean enableDenoising = true;     
        private boolean enableTimeAlignment = true; 

        
        public int getIntervalMinutes() { return intervalMinutes; }
        public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }
        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
        public double getOutlierThreshold() { return outlierThreshold; }
        public void setOutlierThreshold(double outlierThreshold) { this.outlierThreshold = outlierThreshold; }
        public int getMinDataPoints() { return minDataPoints; }
        public void setMinDataPoints(int minDataPoints) { this.minDataPoints = minDataPoints; }
        public boolean isEnableDenoising() { return enableDenoising; }
        public void setEnableDenoising(boolean enableDenoising) { this.enableDenoising = enableDenoising; }
        public boolean isEnableTimeAlignment() { return enableTimeAlignment; }
        public void setEnableTimeAlignment(boolean enableTimeAlignment) { this.enableTimeAlignment = enableTimeAlignment; }
    }

    
    public static class DTWParams {
        private double windowRatio = 0.1;           
        private int maxLookbackPeriod = 96;         
        private int minLookbackPeriod = 24;         
        private boolean useConstrainedDTW = true;   
        private double distanceThreshold = 10.0;    

        
        public double getWindowRatio() { return windowRatio; }
        public void setWindowRatio(double windowRatio) { this.windowRatio = windowRatio; }
        public int getMaxLookbackPeriod() { return maxLookbackPeriod; }
        public void setMaxLookbackPeriod(int maxLookbackPeriod) { this.maxLookbackPeriod = maxLookbackPeriod; }
        public int getMinLookbackPeriod() { return minLookbackPeriod; }
        public void setMinLookbackPeriod(int minLookbackPeriod) { this.minLookbackPeriod = minLookbackPeriod; }
        public boolean isUseConstrainedDTW() { return useConstrainedDTW; }
        public void setUseConstrainedDTW(boolean useConstrainedDTW) { this.useConstrainedDTW = useConstrainedDTW; }
        public double getDistanceThreshold() { return distanceThreshold; }
        public void setDistanceThreshold(double distanceThreshold) { this.distanceThreshold = distanceThreshold; }
    }

    
    public static class MahalanobisParams {
        private double distanceThreshold = 2.5;     
        private boolean useSimplifiedVersion = true; 
        private int minSamplesForCovariance = 30;   
        private double regularizationFactor = 0.01; 

        
        public double getDistanceThreshold() { return distanceThreshold; }
        public void setDistanceThreshold(double distanceThreshold) { this.distanceThreshold = distanceThreshold; }
        public boolean isUseSimplifiedVersion() { return useSimplifiedVersion; }
        public void setUseSimplifiedVersion(boolean useSimplifiedVersion) { this.useSimplifiedVersion = useSimplifiedVersion; }
        public int getMinSamplesForCovariance() { return minSamplesForCovariance; }
        public void setMinSamplesForCovariance(int minSamplesForCovariance) { this.minSamplesForCovariance = minSamplesForCovariance; }
        public double getRegularizationFactor() { return regularizationFactor; }
        public void setRegularizationFactor(double regularizationFactor) { this.regularizationFactor = regularizationFactor; }
    }

    
    public static class ZScoreParams {
        private double threshold = 3.0;             
        private int rollingWindowSize = 30;         
        private boolean useRollingStats = true;     
        private double minStdDev = 0.1;             

        
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
        public int getRollingWindowSize() { return rollingWindowSize; }
        public void setRollingWindowSize(int rollingWindowSize) { this.rollingWindowSize = rollingWindowSize; }
        public boolean isUseRollingStats() { return useRollingStats; }
        public void setUseRollingStats(boolean useRollingStats) { this.useRollingStats = useRollingStats; }
        public double getMinStdDev() { return minStdDev; }
        public void setMinStdDev(double minStdDev) { this.minStdDev = minStdDev; }
    }

    
    public static class ComprehensiveParams {
        private double anomalyThreshold = 0.6;      
        private boolean enableWeightedScoring = true; 
        private int minAlgorithmsForDecision = 2;   
        private double confidenceThreshold = 0.7;   

        
        public double getAnomalyThreshold() { return anomalyThreshold; }
        public void setAnomalyThreshold(double anomalyThreshold) { this.anomalyThreshold = anomalyThreshold; }
        public boolean isEnableWeightedScoring() { return enableWeightedScoring; }
        public void setEnableWeightedScoring(boolean enableWeightedScoring) { this.enableWeightedScoring = enableWeightedScoring; }
        public int getMinAlgorithmsForDecision() { return minAlgorithmsForDecision; }
        public void setMinAlgorithmsForDecision(int minAlgorithmsForDecision) { this.minAlgorithmsForDecision = minAlgorithmsForDecision; }
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    }

    public static class DetectionParams {
        private int featureWindowSize = 5;
        private double alphaBase = 3.0;
        private double sensitivity = 5.0;
        private double dtwStdFactorBase = 2.0;

        public int getFeatureWindowSize() { return featureWindowSize; }
        public void setFeatureWindowSize(int featureWindowSize) { this.featureWindowSize = featureWindowSize; }
        public double getAlphaBase() { return alphaBase; }
        public void setAlphaBase(double alphaBase) { this.alphaBase = alphaBase; }
        public double getSensitivity() { return sensitivity; }
        public void setSensitivity(double sensitivity) { this.sensitivity = sensitivity; }
        public double getDtwStdFactorBase() { return dtwStdFactorBase; }
        public void setDtwStdFactorBase(double dtwStdFactorBase) { this.dtwStdFactorBase = dtwStdFactorBase; }
    }
}
