package com.powermanage.service;

import com.powermanage.entity.Notification;
import com.powermanage.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void sendNotification(String recipient, String message) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setTitle("系统通知");
        n.setContent(message);
        n.setCategory("system");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
        System.out.println("Sending notification to " + recipient + ": " + message);
    }

    public Page<Notification> listNotifications(String recipient, int page, int size) {
        if (recipient != null && !recipient.isEmpty()) {
            return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient, PageRequest.of(page, size));
        }
        return notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public void markRead(Long id) {
        System.out.println("NotificationService.markRead 被调用，ID: " + id);
        notificationRepository.findById(id).ifPresent(n -> {
            System.out.println("找到通知记录，当前已读状态: " + n.getIsRead());
            n.setIsRead(true);
            Notification saved = notificationRepository.save(n);
            System.out.println("通知已读状态保存成功，保存后的状态: " + saved.getIsRead());
        });
    }
}