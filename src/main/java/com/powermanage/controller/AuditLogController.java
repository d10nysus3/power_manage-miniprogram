package com.powermanage.controller;

import com.powermanage.entity.AuditLog;
import com.powermanage.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    public List<AuditLog> getAllAuditLogs() {
        return auditService.getAllAuditLogs();
    }
}