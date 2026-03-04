package com.powermanage.controller;

import com.powermanage.entity.AnomalyDetectionRecord;
import com.powermanage.entity.ElectricityData;
import com.powermanage.entity.UserElectricityPattern;
import com.powermanage.service.AnomalyDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyDetectionController {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @PostMapping("/detect/single")
    public ResponseEntity<Boolean> detectSingleAnomaly(@RequestBody ElectricityData electricityData) {
        boolean isAnomaly = anomalyDetectionService.detectAnomaly(electricityData);
        return ResponseEntity.ok(isAnomaly);
    }

    
    @PostMapping("/detect/batch")
    public ResponseEntity<List<AnomalyDetectionRecord>> batchDetectAnomalies(@RequestBody List<ElectricityData> electricityDataList) {
        List<AnomalyDetectionRecord> anomalyRecords = anomalyDetectionService.batchDetectAnomalies(electricityDataList);
        return ResponseEntity.ok(anomalyRecords);
    }

    
    @PostMapping("/detect/pattern/{userId}")
    public ResponseEntity<List<AnomalyDetectionRecord>> detectAnomaliesBasedOnPattern(
            @PathVariable Long userId, 
            @RequestBody List<ElectricityData> electricityDataList) {
        List<AnomalyDetectionRecord> anomalyRecords = anomalyDetectionService.detectAnomaliesBasedOnPattern(userId, electricityDataList);
        return ResponseEntity.ok(anomalyRecords);
    }

    
    @GetMapping("/user/{userId}")
    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByUserId(@PathVariable Long userId) {
        return anomalyDetectionService.getAnomalyDetectionRecordsByUserId(userId);
    }

    
    @GetMapping("/time-range")
    public ResponseEntity<List<AnomalyDetectionRecord>> getAnomalyDetectionRecordsByTimeRange(
            @RequestParam String startTime, 
            @RequestParam String endTime) {

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
            
            List<AnomalyDetectionRecord> records = anomalyDetectionService.getAnomalyDetectionRecordsByTimeRange(start, end);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    
    @GetMapping("/type/{anomalyType}")
    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByAnomalyType(@PathVariable String anomalyType) {
        return anomalyDetectionService.getAnomalyDetectionRecordsByAnomalyType(anomalyType);
    }

    
    @GetMapping("/algorithm/{algorithm}")
    public List<AnomalyDetectionRecord> getAnomalyDetectionRecordsByAlgorithm(@PathVariable String algorithm) {
        return anomalyDetectionService.getAnomalyDetectionRecordsByAlgorithm(algorithm);
    }

    
    @PutMapping("/algorithm/parameters")
    public ResponseEntity<Void> updateDetectionAlgorithmParameters(
            @RequestParam String algorithm, 
            @RequestParam String parameters) {
        anomalyDetectionService.updateDetectionAlgorithmParameters(algorithm, parameters);
        return ResponseEntity.ok().build();
    }

    
    @PostMapping("/pattern/build/{userId}")
    public ResponseEntity<UserElectricityPattern> buildUserElectricityPattern(@PathVariable Long userId) {
        UserElectricityPattern pattern = anomalyDetectionService.buildUserElectricityPattern(userId);
        return ResponseEntity.ok(pattern);
    }

    
    @GetMapping("/pattern/{userId}")
    public ResponseEntity<UserElectricityPattern> getUserElectricityPattern(
            @PathVariable Long userId, 
            @RequestParam(required = false, defaultValue = "1.0") String version) {
        UserElectricityPattern pattern = anomalyDetectionService.getUserElectricityPattern(userId, version);
        return pattern != null ? ResponseEntity.ok(pattern) : ResponseEntity.notFound().build();
    }

    
    @PostMapping("/pattern/build/batch")
    public ResponseEntity<List<UserElectricityPattern>> batchBuildUserElectricityPatterns(@RequestBody List<Long> userIds) {
        List<UserElectricityPattern> patterns = anomalyDetectionService.batchBuildUserElectricityPatterns(userIds);
        return ResponseEntity.ok(patterns);
    }

    
    @GetMapping("/statistics")
    public ResponseEntity<Void> generateAnomalyStatistics(
            @RequestParam String startTime, 
            @RequestParam String endTime) {

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
            
            anomalyDetectionService.generateAnomalyStatistics(start, end);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}