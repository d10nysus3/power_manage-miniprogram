package com.powermanage.controller;

import com.powermanage.service.SystemManagementService;
import com.powermanage.utils.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/system-management")
public class SystemManagementController {
    private static final Logger logger = LoggerFactory.getLogger(SystemManagementController.class);

    @Autowired
    private SystemManagementService systemManagementService;

    
    @PostMapping("/clear-all-data")
    public Map<String, Object> clearAllData() {
        try {
            systemManagementService.clearAllData();
            return ResponseUtil.success("所有数据已清空");
        } catch (Exception e) {
            logger.error("清空数据失败: {}", e.getMessage(), e);
            return ResponseUtil.error("清空数据失败: " + e.getMessage());
        }
    }

    
    @GetMapping("/backup-data")
    public void backupData(HttpServletResponse response) {
        try {
            systemManagementService.backupData(response);
        } catch (Exception e) {
            logger.error("备份数据失败: {}", e.getMessage(), e);
        }
    }

    
    @PostMapping("/generate-random-data")
    public Map<String, Object> generateRandomData() {
        try {
            systemManagementService.generateRandomData();
            return ResponseUtil.success("随机数据已生成");
        } catch (Exception e) {
            logger.error("生成随机数据失败: {}", e.getMessage(), e);
            return ResponseUtil.error("生成随机数据失败: " + e.getMessage());
        }
    }
}