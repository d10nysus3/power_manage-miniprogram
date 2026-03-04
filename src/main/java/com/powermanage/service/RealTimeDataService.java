package com.powermanage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class RealTimeDataService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 5000)
    public void sendDataUpdates() {
        
        String data = "Current data at " + System.currentTimeMillis();
        messagingTemplate.convertAndSend("/topic/data", data);
    }
}