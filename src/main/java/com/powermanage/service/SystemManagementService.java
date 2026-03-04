package com.powermanage.service;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;

@Service
public interface SystemManagementService {
    
    
    void clearAllData();
    
    
    void backupData(HttpServletResponse response);
    
    
    void generateRandomData();
}