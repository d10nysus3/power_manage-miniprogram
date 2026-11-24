package com.powermanage.controller;

import com.powermanage.entity.AuditLog;
import com.powermanage.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    public Page<AuditLog> getAllAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long minId,
            @RequestParam(required = false) Long maxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if ((startDate != null && endDate != null && startDate.isAfter(endDate)) ||
                (minId != null && maxId != null && minId > maxId) ||
                page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询范围或分页参数无效");
        }
        return auditService.search(startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                minId, maxId, page, size);
    }

    @GetMapping("/range")
    public Map<String, Object> getRange() {
        Object[] range = auditService.getRange();
        return Map.of("count", range[4], "earliestTime", range[0] == null ? "" : range[0],
                "latestTime", range[1] == null ? "" : range[1],
                "minId", range[2] == null ? 0 : range[2],
                "maxId", range[3] == null ? 0 : range[3]);
    }
}
