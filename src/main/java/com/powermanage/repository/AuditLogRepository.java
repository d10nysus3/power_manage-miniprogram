package com.powermanage.repository;

import com.powermanage.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE (:startTime IS NULL OR a.timestamp >= :startTime) " +
            "AND (:endTime IS NULL OR a.timestamp < :endTime) " +
            "AND (:minId IS NULL OR a.id >= :minId) AND (:maxId IS NULL OR a.id <= :maxId)")
    Page<AuditLog> search(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("minId") Long minId,
                          @Param("maxId") Long maxId,
                          Pageable pageable);

    @Query("SELECT MIN(a.timestamp), MAX(a.timestamp), MIN(a.id), MAX(a.id), COUNT(a) FROM AuditLog a")
    Object[] getRange();

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoff")
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
