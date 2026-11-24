package com.powermanage.service;

import com.powermanage.entity.Setting;
import com.powermanage.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingService {
    public static final String AUDIT_RETENTION_KEY = "audit.retention.days";
    public static final int DEFAULT_AUDIT_RETENTION_DAYS = 365;

    @Autowired
    private SettingRepository settingRepository;

    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    public int getAuditRetentionDays() {
        return settingRepository.findByKey(AUDIT_RETENTION_KEY)
                .map(setting -> Integer.parseInt(setting.getValue()))
                .orElse(DEFAULT_AUDIT_RETENTION_DAYS);
    }

    public Setting updateSetting(String key, String value) {
        if (AUDIT_RETENTION_KEY.equals(key)) {
            try {
                int days = Integer.parseInt(value);
                if (days < 1 || days > 3650) {
                    throw new IllegalArgumentException("审计日志留存天数须在 1 至 3650 天之间");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("审计日志留存天数须为整数");
            }
        }
        
        java.util.Optional<Setting> existingSettingOpt = settingRepository.findByKey(key);
        
        if (existingSettingOpt.isPresent()) {
            
            Setting existingSetting = existingSettingOpt.get();
            existingSetting.setValue(value);
            return settingRepository.save(existingSetting);
        } else {
            
            Setting setting = new Setting();
            setting.setKey(key);
            setting.setValue(value);
            return settingRepository.save(setting);
        }
    }
}
