package com.powermanage.task;

import com.powermanage.service.AnomalyDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AnomalyDetectionScheduler {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    /**
     * 定时任务：每隔 1 分钟扫描一次最近24小时内的"实时"未检测数据进行补测
     * 对应需求：每隔一段时间会对data_type='实时'的未检验的数据进行数据异常检验算法
     * 调整为 1 分钟是为了更快地响应数据库直接插入的数据（SQL注入场景）
     */
    @Scheduled(fixedRate = 60000) // 1分钟执行一次
    public void scheduledRealtimeAnomalyDetection() {
        System.out.println(">>> Scheduled Task: Starting real-time anomaly detection scan...");
        // 扫描最近 24 小时的数据，防止漏检
        LocalDateTime scanStartTime = LocalDateTime.now().minusHours(24);
        anomalyDetectionService.scanAndDetectUnprocessedData(scanStartTime);
        System.out.println(">>> Scheduled Task: Real-time anomaly detection scan completed.");
    }
}
