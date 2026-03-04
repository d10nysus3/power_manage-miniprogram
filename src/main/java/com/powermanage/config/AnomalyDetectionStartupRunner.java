package com.powermanage.config;

import com.powermanage.service.AnomalyDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AnomalyDetectionStartupRunner implements CommandLineRunner {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> System Startup: Initiating historical anomaly detection scan...");
        // 扫描最近7天的数据，确保初始化脚本(init_database.sql)插入的数据(通常是最近几天)能被检测到
        // 即使系统时间比数据时间晚24小时，这种回溯扫描也能覆盖到
        LocalDateTime scanStartTime = LocalDateTime.now().minusDays(7);
        anomalyDetectionService.scanAndDetectUnprocessedData(scanStartTime);
        System.out.println(">>> System Startup: Historical scan completed.");

        // 检查并补全缺失的用户画像
        System.out.println(">>> System Startup: Checking for missing user patterns...");
        anomalyDetectionService.checkAndBuildMissingPatterns();
        System.out.println(">>> System Startup: Pattern check completed.");
    }
}
