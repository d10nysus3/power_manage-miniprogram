package com.powermanage.repository;

import com.powermanage.entity.ElectricityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ElectricityDataRepository extends JpaRepository<ElectricityData, Long> {
    
    List<ElectricityData> findByUser_IdAndRecordTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<ElectricityData> findByRecordTimeAfter(LocalDateTime startTime);

    List<ElectricityData> findByUser_IdAndDataTypeAndRecordTimeAfter(Long userId, String dataType, LocalDateTime startTime);
    
    List<ElectricityData> findByDataTypeAndRecordTimeAfter(String dataType, LocalDateTime startTime);
    
    List<ElectricityData> findByUser_IdAndDataType(Long userId, String dataType);

    ElectricityData findTopByUser_IdOrderByRecordTimeDesc(Long userId);

    List<ElectricityData> findByRecordTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<ElectricityData> findByRecordTimeBetweenAndIsValidTrue(LocalDateTime startTime, LocalDateTime endTime);
    
    List<ElectricityData> findByUser_IdAndRecordTimeAfter(Long userId, LocalDateTime recordTime);
    
    List<ElectricityData> findByUser_Id(Long userId);
    
    List<ElectricityData> findTop10ByOrderByRecordTimeDesc();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT e.user.id FROM ElectricityData e WHERE e.dataType = ?1")
    List<Long> findUserIdsByDataType(String dataType);
}