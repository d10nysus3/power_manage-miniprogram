package com.powermanage.controller;

import com.powermanage.service.RealtimeAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private RealtimeAlertService realtimeAlertService;

    
    @MessageMapping("/connect")
    @SendTo("/topic/connection")
    public Map<String, Object> handleConnection(Map<String, Object> message) {
        try {
            Long userId = Long.valueOf(message.get("userId").toString());
            
            
            realtimeAlertService.userOnline(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "connection_established");
            response.put("userId", userId);
            response.put("onlineUserCount", realtimeAlertService.getOnlineUserCount());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("用户连接成功: ID={}", userId);
            return response;
            
        } catch (Exception e) {
            logger.error("处理用户连接失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "connection_error");
            errorResponse.put("error", "连接失败");
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorResponse;
        }
    }

    
    @MessageMapping("/disconnect")
    @SendTo("/topic/connection")
    public Map<String, Object> handleDisconnection(Map<String, Object> message) {
        try {
            Long userId = Long.valueOf(message.get("userId").toString());
            
            
            realtimeAlertService.userOffline(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "connection_closed");
            response.put("userId", userId);
            response.put("onlineUserCount", realtimeAlertService.getOnlineUserCount());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("用户断开连接: ID={}", userId);
            return response;
            
        } catch (Exception e) {
            logger.error("处理用户断开连接失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "disconnection_error");
            errorResponse.put("error", "断开连接失败");
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorResponse;
        }
    }

    
    @MessageMapping("/heartbeat")
    @SendTo("/topic/heartbeat")
    public Map<String, Object> handleHeartbeat(Map<String, Object> message) {
        try {
            Long userId = Long.valueOf(message.get("userId").toString());
            
            
            realtimeAlertService.updateUserActivity(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "heartbeat_response");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理心跳消息失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "heartbeat_error");
            errorResponse.put("error", "心跳处理失败");
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorResponse;
        }
    }

    
    @MessageMapping("/acknowledge-alert")
    @SendTo("/topic/alert-acknowledgment")
    public Map<String, Object> handleAlertAcknowledgment(Map<String, Object> message) {
        try {
            Long alertId = Long.valueOf(message.get("alertId").toString());
            Long userId = Long.valueOf(message.get("userId").toString());
            String acknowledgment = message.get("acknowledgment").toString();
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "alert_acknowledged");
            response.put("alertId", alertId);
            response.put("userId", userId);
            response.put("acknowledgment", acknowledgment);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("告警确认: 告警ID={}, 用户ID={}, 确认信息={}", alertId, userId, acknowledgment);
            return response;
            
        } catch (Exception e) {
            logger.error("处理告警确认失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "acknowledgment_error");
            errorResponse.put("error", "告警确认失败");
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorResponse;
        }
    }

    
    @SubscribeMapping("/status")
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("type", "connection_status");
        status.put("onlineUserCount", realtimeAlertService.getOnlineUserCount());
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return status;
    }
}
