package com.powermanage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.powermanage.service.ElectricityDataService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
public class ReportService {
    
    @Autowired
    private ElectricityDataService electricityDataService;

    public Map<String, Object> generateReport(String reportType, String format) {
        
        String generatedReportId = electricityDataService.generateReport(
            null,  
            LocalDateTime.now().minusDays(30),  
            LocalDateTime.now(),  
            reportType  
        );
        
        
        String reportUrl = "/api/electricity-data/download-report/" + generatedReportId + 
                          "?format=" + (format != null ? format.toUpperCase() : "TEXT");
        
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", generatedReportId);
        result.put("reportUrl", reportUrl);
        
        return result;
    }
    
    public Map<String, Object> generateUserReport(Long userId, String reportType, String format) {
        
        String generatedReportId = electricityDataService.generateReport(
            userId,  
            LocalDateTime.now().minusDays(30),  
            LocalDateTime.now(),  
            reportType  
        );
        
        
        String reportUrl = "/api/electricity-data/download-report/" + generatedReportId + 
                          "?format=" + (format != null ? format.toUpperCase() : "TEXT");
        
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", generatedReportId);
        result.put("reportUrl", reportUrl);
        
        return result;
    }
}