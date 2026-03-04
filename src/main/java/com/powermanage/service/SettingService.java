package com.powermanage.service;

import com.powermanage.entity.Setting;
import com.powermanage.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingService {

    @Autowired
    private SettingRepository settingRepository;

    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    public Setting updateSetting(String key, String value) {
        
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