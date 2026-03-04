package com.powermanage.util;

import com.powermanage.entity.ElectricityData;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;


@Component
public class DataPreprocessingUtil {

    
    private static final int DEFAULT_INTERVAL_MINUTES = 15;

    
    public List<ElectricityData> cleanData(List<ElectricityData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        // 
        List<ElectricityData> validData = dataList.stream()
                // Treat null as true (default behavior)
                .filter(data -> data.getIsValid() == null || data.getIsValid())
                .collect(Collectors.toList());

        return validData;
    }

    
    public List<ElectricityData> denoiseData(List<ElectricityData> dataList, int windowSize) {
        if (dataList == null || dataList.isEmpty() || windowSize <= 1) {
            return new ArrayList<>(dataList);
        }

        
        List<ElectricityData> sortedData = new ArrayList<>(dataList);
        sortedData.sort((a, b) -> a.getRecordTime().compareTo(b.getRecordTime()));

        List<ElectricityData> denoisedData = new ArrayList<>();
        int halfWindow = windowSize / 2;

        for (int i = 0; i < sortedData.size(); i++) {
            ElectricityData originalData = sortedData.get(i);
            ElectricityData denoised = new ElectricityData();
            denoised.setId(originalData.getId());
            denoised.setUser(originalData.getUser());
            denoised.setRecordTime(originalData.getRecordTime());
            denoised.setDataSource(originalData.getDataSource());
            denoised.setDataType(originalData.getDataType());
            denoised.setIsValid(originalData.getIsValid());

            
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(sortedData.size(), i + halfWindow + 1);
            List<ElectricityData> windowData = sortedData.subList(start, end);

            double avgPower = windowData.stream()
                    .mapToDouble(ElectricityData::getPower)
                    .average()
                    .orElse(originalData.getPower());

            double avgConsumption = windowData.stream()
                    .mapToDouble(ElectricityData::getElectricityConsumption)
                    .average()
                    .orElse(originalData.getElectricityConsumption());

            denoised.setPower(avgPower);
            denoised.setElectricityConsumption(avgConsumption);
            denoisedData.add(denoised);
        }

        return denoisedData;
    }

    
    public List<ElectricityData> alignDataByTime(List<ElectricityData> dataList, int intervalMinutes) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        
        if (intervalMinutes <= 0) {
            intervalMinutes = DEFAULT_INTERVAL_MINUTES;
        }

        
        List<ElectricityData> sortedData = new ArrayList<>(dataList);
        sortedData.sort((a, b) -> a.getRecordTime().compareTo(b.getRecordTime()));

        
        LocalDateTime startTime = sortedData.get(0).getRecordTime();
        LocalDateTime endTime = sortedData.get(sortedData.size() - 1).getRecordTime();

        
        Map<LocalDateTime, ElectricityData> alignedMap = new TreeMap<>();
        ElectricityData userData = sortedData.get(0);

        
        LocalDateTime currentTime = alignToInterval(startTime, intervalMinutes);
        while (currentTime.isBefore(endTime.plusMinutes(intervalMinutes))) {
            
            ElectricityData closestData = findClosestData(sortedData, currentTime);
            if (closestData != null) {
                ElectricityData alignedData = new ElectricityData();
                alignedData.setUser(closestData.getUser());
                alignedData.setRecordTime(currentTime);
                alignedData.setPower(closestData.getPower());
                alignedData.setElectricityConsumption(closestData.getElectricityConsumption());
                alignedData.setDataSource(closestData.getDataSource());
                alignedData.setDataType(closestData.getDataType());
                alignedData.setIsValid(closestData.getIsValid());
                alignedMap.put(currentTime, alignedData);
            }
            
            currentTime = currentTime.plusMinutes(intervalMinutes);
        }

        
        return new ArrayList<>(alignedMap.values());
    }

    
    private LocalDateTime alignToInterval(LocalDateTime time, int intervalMinutes) {
        int minutes = time.getMinute();
        int alignedMinutes = (minutes / intervalMinutes) * intervalMinutes;
        return time.withMinute(alignedMinutes).withSecond(0).withNano(0);
    }

    
    private ElectricityData findClosestData(List<ElectricityData> dataList, LocalDateTime targetTime) {
        if (dataList.isEmpty()) {
            return null;
        }

        ElectricityData closestData = null;
        long minDifference = Long.MAX_VALUE;

        for (ElectricityData data : dataList) {
            long difference = Math.abs(targetTime.until(data.getRecordTime(), java.time.temporal.ChronoUnit.MINUTES));
            if (difference < minDifference) {
                minDifference = difference;
                closestData = data;
            }
        }

        
        return minDifference <= 30 ? closestData : null;
    }

    
    public List<ElectricityData> preprocessData(List<ElectricityData> dataList, int intervalMinutes, int windowSize) {
        
        List<ElectricityData> cleanedData = cleanData(dataList);
        
        // 
        // List<ElectricityData> denoisedData = denoiseData(cleanedData, windowSize);
        
        // 
        List<ElectricityData> alignedData = alignDataByTime(cleanedData, intervalMinutes);
        
        return alignedData;
    }

    
    public List<ElectricityData> detectAndMarkAnomalies(List<ElectricityData> dataList) {
        List<ElectricityData> result = new ArrayList<>();
        List<ElectricityData> cleanedData = cleanData(dataList);

        
        Map<Long, ElectricityData> originalDataMap = dataList.stream()
                .collect(Collectors.toMap(ElectricityData::getId, d -> d));

        
        Set<Long> validIds = cleanedData.stream()
                .map(ElectricityData::getId)
                .collect(Collectors.toSet());

        
        for (ElectricityData originalData : dataList) {
            ElectricityData markedData = new ElectricityData();
            markedData = originalData;
            markedData.setIsValid(validIds.contains(originalData.getId()));
            result.add(markedData);
        }

        return result;
    }
}