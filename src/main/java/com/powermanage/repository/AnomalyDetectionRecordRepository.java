package com.powermanage.repository;

import com.powermanage.entity.AnomalyDetectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyDetectionRecordRepository extends JpaRepository<AnomalyDetectionRecord, Long> {
    
    List<AnomalyDetectionRecord> findByUser_IdAndDetectionTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    
    List<AnomalyDetectionRecord> findByIsAnomalyTrue();

    
    List<AnomalyDetectionRecord> findByDetectionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    
    List<AnomalyDetectionRecord> findByUser_Id(Long userId);
    
    
    List<AnomalyDetectionRecord> findByAnomalyType(String anomalyType);
    
    
    List<AnomalyDetectionRecord> findByAlgorithm(String algorithm);
    
    
    List<AnomalyDetectionRecord> findByDetectionDataId(Long detectionDataId);

    boolean existsByDetectionDataId(Long detectionDataId);

    
    AnomalyDetectionRecord findTopByUser_IdOrderByDetectionTimeDesc(Long userId);
    
    
    @Query("SELECT r FROM AnomalyDetectionRecord r JOIN FETCH r.user WHERE r.detectionTime BETWEEN ?1 AND ?2")
    List<AnomalyDetectionRecord> findByDetectionTimeBetweenWithUser(LocalDateTime startTime, LocalDateTime endTime);
    
    
    long countByDetectionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    
    long countByDetectionTimeBetweenAndIsHandledTrue(LocalDateTime startTime, LocalDateTime endTime);

    
    @Query(value = "SELECT * FROM t_anomaly_detection_record ORDER BY detection_time DESC LIMIT ?1", nativeQuery = true)
    List<AnomalyDetectionRecord> findTopNByOrderByDetectionTimeDesc(int limit);

    
    @Query("SELECT r.anomalyType as anomalyType, count(r) as value FROM AnomalyDetectionRecord r GROUP BY r.anomalyType")
    List<java.util.Map<String, Object>> countByAnomalyType();

    List<AnomalyDetectionRecord> findByUser_IdAndAnomalyTypeAndDetectionTimeAfter(Long userId, String anomalyType, LocalDateTime detectionTime);
}