package com.powermanage.service.impl;

import com.powermanage.entity.AlertRecord;
import com.powermanage.entity.AnomalyDetectionRecord;
import com.powermanage.entity.User;
import com.powermanage.service.RealtimeAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class RealtimeAlertServiceImpl implements RealtimeAlertService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeAlertServiceImpl.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    
    private final Map<Long, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();
    
    
    private static final String ALERT_TOPIC = "/topic/alerts";
    private static final String NOTIFICATION_TOPIC = "/topic/notifications";
    private static final String MONITORING_TOPIC = "/topic/monitoring";
    private static final String USER_ALERT_TOPIC = "/queue/user-alerts";

    @Override
    @org.springframework.scheduling.annotation.Async
    public void pushAnomalyAlert(AnomalyDetectionRecord anomalyRecord, User user) {
        try {
            Map<String, Object> alertMessage = new HashMap<>();
            alertMessage.put("type", "anomaly_alert");
            alertMessage.put("id", anomalyRecord.getId());
            if (user != null) {
                alertMessage.put("userId", user.getId());
                alertMessage.put("userName", user.getName());
                alertMessage.put("userPhone", user.getPhoneNumber());
            } else {
                alertMessage.put("userId", anomalyRecord.getUserId());
                alertMessage.put("userName", "未知用户");
                alertMessage.put("userPhone", "N/A");
            }
            alertMessage.put("anomalyType", anomalyRecord.getAnomalyType());
            alertMessage.put("severityLevel", anomalyRecord.getSeverityLevel());
            alertMessage.put("anomalyScore", anomalyRecord.getAnomalyScore());
            alertMessage.put("detectionTime", anomalyRecord.getDetectionTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            alertMessage.put("notes", anomalyRecord.getNotes());
            alertMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            
            messagingTemplate.convertAndSend(ALERT_TOPIC, alertMessage);
            
            if (user != null) {
                messagingTemplate.convertAndSendToUser(
                    user.getId().toString(), 
                    USER_ALERT_TOPIC, 
                    alertMessage
                );
                logger.info("异常告警推送成功: 用户ID={}, 异常类型={}, 严重程度={}", 
                    user.getId(), anomalyRecord.getAnomalyType(), anomalyRecord.getSeverityLevel());
            } else {
                logger.info("异常告警广播推送成功 (未找到对应用户信息): 异常类型={}, 严重程度={}", 
                    anomalyRecord.getAnomalyType(), anomalyRecord.getSeverityLevel());
            }

        } catch (Exception e) {
            logger.error("推送异常告警失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void pushAlertStatusUpdate(AlertRecord alertRecord) {
        try {
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("type", "alert_status_update");
            statusMessage.put("alertId", alertRecord.getId());
            statusMessage.put("userId", alertRecord.getUserId());
            statusMessage.put("alertType", alertRecord.getAlertType());
            statusMessage.put("status", alertRecord.getStatus());
            statusMessage.put("processTime", alertRecord.getProcessTime() != null ? 
                alertRecord.getProcessTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            statusMessage.put("processResult", alertRecord.getProcessResult());
            statusMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            
            messagingTemplate.convertAndSend(ALERT_TOPIC, statusMessage);
            
            
            messagingTemplate.convertAndSendToUser(
                alertRecord.getUserId().toString(), 
                USER_ALERT_TOPIC, 
                statusMessage
            );

            logger.info("告警状态更新推送成功: 告警ID={}, 状态={}", 
                alertRecord.getId(), alertRecord.getStatus());

        } catch (Exception e) {
            logger.error("推送告警状态更新失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void pushSystemNotification(String message, List<Long> targetUsers) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "system_notification");
            notification.put("message", message);
            notification.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            if (targetUsers == null || targetUsers.isEmpty()) {
                
                messagingTemplate.convertAndSend(NOTIFICATION_TOPIC, notification);
                logger.info("系统通知推送成功: {}", message);
            } else {
                
                for (Long userId : targetUsers) {
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(), 
                        USER_ALERT_TOPIC, 
                        notification
                    );
                }
                logger.info("系统通知推送成功: 目标用户数={}, 消息={}", targetUsers.size(), message);
            }

        } catch (Exception e) {
            logger.error("推送系统通知失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void pushMonitoringData(Map<String, Object> monitoringData) {
        try {
            Map<String, Object> data = new HashMap<>(monitoringData);
            data.put("type", "monitoring_data");
            data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            messagingTemplate.convertAndSend(MONITORING_TOPIC, data);
            
            logger.debug("实时监控数据推送成功");

        } catch (Exception e) {
            logger.error("推送实时监控数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public int getOnlineUserCount() {
        
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(5);
        onlineUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(expireTime));
        
        return onlineUsers.size();
    }

    @Override
    public boolean isUserOnline(Long userId) {
        LocalDateTime lastActivity = onlineUsers.get(userId);
        if (lastActivity == null) {
            return false;
        }
        
        
        return lastActivity.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    
    public void userOnline(Long userId) {
        onlineUsers.put(userId, LocalDateTime.now());
        logger.info("用户上线: ID={}", userId);
    }

    
    public void userOffline(Long userId) {
        onlineUsers.remove(userId);
        logger.info("用户离线: ID={}", userId);
    }

    
    public void updateUserActivity(Long userId) {
        onlineUsers.put(userId, LocalDateTime.now());
    }

    
    public List<Long> getOnlineUsers() {
        
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(5);
        onlineUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(expireTime));
        
        return new ArrayList<>(onlineUsers.keySet());
    }

    
@Override
    @org.springframework.scheduling.annotation.Async
    public void pushUrgentAlert(AlertRecord alertRecord, User user) {
        try {
            Map<String, Object> urgentAlert = new HashMap<>();
            urgentAlert.put("type", "urgent_alert");
            urgentAlert.put("priority", "high");
            urgentAlert.put("alertId", alertRecord.getId());
            urgentAlert.put("userId", user.getId());
            urgentAlert.put("userName", user.getName());
            urgentAlert.put("userPhone", user.getPhoneNumber());
            urgentAlert.put("alertType", alertRecord.getAlertType());
            urgentAlert.put("severityLevel", alertRecord.getSeverityLevel());
            urgentAlert.put("triggerReason", alertRecord.getTriggerReason());
            urgentAlert.put("alertTime", alertRecord.getAlertTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            urgentAlert.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            
            messagingTemplate.convertAndSend(ALERT_TOPIC, urgentAlert);
            
            
            messagingTemplate.convertAndSendToUser(
                user.getId().toString(), 
                USER_ALERT_TOPIC, 
                urgentAlert
            );

            
            String notification = String.format("紧急告警: 用户%s(%s)发生%s异常", 
                user.getName(), user.getPhoneNumber(), alertRecord.getAlertType());
            pushSystemNotification(notification, null);

            logger.warn("紧急告警推送成功: 用户ID={}, 告警类型={}, 严重程度={}", 
                user.getId(), alertRecord.getAlertType(), alertRecord.getSeverityLevel());

        } catch (Exception e) {
            logger.error("推送紧急告警失败: {}", e.getMessage(), e);
        }
    }
}
