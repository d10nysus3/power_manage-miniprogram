package com.powermanage.controller;

import com.powermanage.entity.Setting;
import com.powermanage.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingController {
    private static final Logger logger = LoggerFactory.getLogger(SettingController.class);

    @Autowired
    private SettingService settingService;

    @GetMapping
    public List<Setting> getAllSettings() {
        try {
            List<Setting> settings = settingService.getAllSettings();
            
            if (settings.isEmpty()) {
                logger.info("No settings found in database, returning default settings");
                List<Setting> defaultSettings = new ArrayList<>();
                defaultSettings.add(createDefaultSetting("system.name", "用电异常监测与管理系统"));
                defaultSettings.add(createDefaultSetting("system.version", "1.0.0"));
                defaultSettings.add(createDefaultSetting("alert.threshold.high", "100"));
                defaultSettings.add(createDefaultSetting("alert.threshold.medium", "80"));
                defaultSettings.add(createDefaultSetting("alert.threshold.low", "60"));
                return defaultSettings;
            }
            return settings;
        } catch (Exception e) {
            logger.error("Error getting settings: {}", e.getMessage());
            
            List<Setting> defaultSettings = new ArrayList<>();
            defaultSettings.add(createDefaultSetting("system.name", "用电异常监测与管理系统"));
            defaultSettings.add(createDefaultSetting("system.version", "1.0.0"));
            return defaultSettings;
        }
    }
    
    private Setting createDefaultSetting(String key, String value) {
        Setting setting = new Setting();
        setting.setKey(key);
        setting.setValue(value);
        return setting;
    }

    @PostMapping
    public Map<String, Object> updateSetting(@RequestBody Map<String, String> payload) {
        try {
            String key = payload.get("key");
            String value = payload.get("value");
            settingService.updateSetting(key, value);
            return Map.of("success", true, "message", "Setting updated successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}