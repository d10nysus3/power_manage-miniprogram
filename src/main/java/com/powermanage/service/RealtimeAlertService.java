package com.powermanage.service;

import com.powermanage.entity.AlertRecord;
import com.powermanage.entity.AnomalyDetectionRecord;
import com.powermanage.entity.User;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public interface RealtimeAlertService {

    
    void pushAnomalyAlert(AnomalyDetectionRecord anomalyRecord, User user);

    
    void pushAlertStatusUpdate(AlertRecord alertRecord);

    
    void pushSystemNotification(String message, List<Long> targetUsers);

    
    void pushMonitoringData(Map<String, Object> monitoringData);

    
    int getOnlineUserCount();

    
    boolean isUserOnline(Long userId);

    
    void userOnline(Long userId);

    
    void userOffline(Long userId);

    void updateUserActivity(Long userId);

    void pushUrgentAlert(AlertRecord alertRecord, User user);
}
