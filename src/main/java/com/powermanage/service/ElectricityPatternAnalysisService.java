package com.powermanage.service;

import com.powermanage.entity.ElectricityData;
import com.powermanage.entity.User;
import com.powermanage.entity.UserElectricityPattern;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public interface ElectricityPatternAnalysisService {

    
    UserElectricityPattern generateAndSaveUserPattern(Long userId);

    UserElectricityPattern getUserPattern(Long userId);

    ElectricityPatternAnalysisResult analyzeUserPattern(User user, List<ElectricityData> historicalData);

    
    UserElectricityPattern buildUserPattern(User user, List<ElectricityData> historicalData);

    
    UserElectricityPattern updateUserPattern(Long userId, List<ElectricityData> newData);

    
    List<String> generatePatternTags(User user, UserElectricityPattern pattern);

    
    Map<String, Object> predictElectricityUsage(Long userId, int period);

    
    Map<String, Object> analyzePatternTrend(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    
    class ElectricityPatternAnalysisResult {
        private UserElectricityPattern pattern;
        private List<String> patternTags;
        private Map<String, Object> statistics;
        private Map<String, Object> dailyPattern;
        private Map<String, Object> weeklyPattern;
        private Map<String, Object> monthlyPattern;
        private String analysisNotes;
        private LocalDateTime analysisTime;

        public ElectricityPatternAnalysisResult() {
            this.analysisTime = LocalDateTime.now();
        }

        
        public UserElectricityPattern getPattern() { return pattern; }
        public void setPattern(UserElectricityPattern pattern) { this.pattern = pattern; }
        public List<String> getPatternTags() { return patternTags; }
        public void setPatternTags(List<String> patternTags) { this.patternTags = patternTags; }
        public Map<String, Object> getStatistics() { return statistics; }
        public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
        public Map<String, Object> getDailyPattern() { return dailyPattern; }
        public void setDailyPattern(Map<String, Object> dailyPattern) { this.dailyPattern = dailyPattern; }
        public Map<String, Object> getWeeklyPattern() { return weeklyPattern; }
        public void setWeeklyPattern(Map<String, Object> weeklyPattern) { this.weeklyPattern = weeklyPattern; }
        public Map<String, Object> getMonthlyPattern() { return monthlyPattern; }
        public void setMonthlyPattern(Map<String, Object> monthlyPattern) { this.monthlyPattern = monthlyPattern; }
        public String getAnalysisNotes() { return analysisNotes; }
        public void setAnalysisNotes(String analysisNotes) { this.analysisNotes = analysisNotes; }
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
    }
}
