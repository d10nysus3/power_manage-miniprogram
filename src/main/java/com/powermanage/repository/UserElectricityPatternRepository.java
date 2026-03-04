package com.powermanage.repository;

import com.powermanage.entity.UserElectricityPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserElectricityPatternRepository extends JpaRepository<UserElectricityPattern, Long> {
    
    Optional<UserElectricityPattern> findByUser_Id(Long userId);

    
    List<UserElectricityPattern> findByAnalysisVersion(Integer version);

    
    List<UserElectricityPattern> findByLastAnalysisDateAfter(LocalDate date);

    
    List<UserElectricityPattern> findByAnalysisAlgorithm(String algorithm);
    
    
    List<UserElectricityPattern> findByAlgorithm(String algorithm);
    
    
    List<UserElectricityPattern> findTopByUser_IdOrderByAnalysisDateDesc(Long userId);
    
    
    List<UserElectricityPattern> findByUser_IdAndVersion(Long userId, String version);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.user.id FROM UserElectricityPattern p")
    List<Long> findAllUserIdsWithPatterns();
}