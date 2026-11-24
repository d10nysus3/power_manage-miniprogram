package com.powermanage.service;

import com.powermanage.repository.AuditLogRepository;
import com.powermanage.repository.SettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditRetentionTest {
    @Test
    void rejectsInvalidRetentionDays() {
        SettingService service = new SettingService();
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSetting(SettingService.AUDIT_RETENTION_KEY, "0"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSetting(SettingService.AUDIT_RETENTION_KEY, "abc"));
    }

    @Test
    void cleanupUsesConfiguredRetention() {
        SettingRepository settings = mock(SettingRepository.class);
        when(settings.findByKey(SettingService.AUDIT_RETENTION_KEY)).thenReturn(Optional.empty());
        SettingService settingService = new SettingService();
        ReflectionTestUtils.setField(settingService, "settingRepository", settings);
        assertEquals(365, settingService.getAuditRetentionDays());

        AuditLogRepository logs = mock(AuditLogRepository.class);
        AuditService auditService = new AuditService();
        ReflectionTestUtils.setField(auditService, "auditLogRepository", logs);
        ReflectionTestUtils.setField(auditService, "settingService", settingService);
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDateTime before = LocalDateTime.now(zone).minusDays(365);
        auditService.deleteExpiredLogs();
        LocalDateTime after = LocalDateTime.now(zone).minusDays(365);
        verify(logs).deleteBefore(org.mockito.ArgumentMatchers.argThat(
                cutoff -> !cutoff.isBefore(before) && !cutoff.isAfter(after)));
    }
}
