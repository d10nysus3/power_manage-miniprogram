package com.powermanage.service;

import com.powermanage.entity.AuditLog;
import com.powermanage.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final ZoneId LOG_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SettingService settingService;

    public void logAction(String username, String action) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setTimestamp(LocalDateTime.now(LOG_ZONE));
        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    public Page<AuditLog> search(LocalDateTime startTime, LocalDateTime endTime,
                                 Long minId, Long maxId, int page, int size) {
        return auditLogRepository.search(startTime, endTime, minId, maxId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    public Object[] getRange() {
        return auditLogRepository.getRange();
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Shanghai")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void deleteExpiredLogs() {
        int days = settingService.getAuditRetentionDays();
        int deleted = auditLogRepository.deleteBefore(LocalDateTime.now(LOG_ZONE).minusDays(days));
        if (deleted > 0) {
            logger.info("Deleted {} audit logs older than {} days", deleted, days);
        }
    }
}
