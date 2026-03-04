package com.powermanage.controller;

import com.powermanage.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, String> payload) {
        try {
            String recipient = payload.get("recipient");
            String message = payload.get("message");
            notificationService.sendNotification(recipient, message);
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification sent successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    public Object listNotifications(
            @RequestParam(required = false) String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return notificationService.listNotifications(recipient, page, size);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        try {
            System.out.println("标记通知为已读，通知ID: " + id);
            notificationService.markRead(id);
            System.out.println("通知已读状态保存成功，ID: " + id);
            return Map.of("success", true);
        } catch (Exception e) {
            System.out.println("标记通知已读失败: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}