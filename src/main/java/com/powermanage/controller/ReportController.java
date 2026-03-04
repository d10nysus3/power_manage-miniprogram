package com.powermanage.controller;

import com.powermanage.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    public Map<String, Object> generateReport(@RequestBody Map<String, Object> requestParams) {
        try {
            String reportType = (String) requestParams.get("reportType");
            String format = (String) requestParams.getOrDefault("format", "TEXT");
            
            Map<String, Object> reportData = reportService.generateReport(reportType, format);
            return Map.of("success", true, "data", reportData);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}