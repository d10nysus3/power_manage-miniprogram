package com.powermanage.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class RealTimeDataController {

    @MessageMapping("/request-data")
    @SendTo("/topic/data")
    public String requestData(String message) throws Exception {
        
        
        return "Real-time data update!";
    }
}