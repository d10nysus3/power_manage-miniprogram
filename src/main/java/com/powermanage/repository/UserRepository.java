package com.powermanage.repository;

import com.powermanage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByIdCard(String idCard);

    
    Optional<User> findByMeterNumber(String meterNumber);

    
    List<User> findByCommunity(String community);

    
    List<User> findByIsSingleAloneTrue();
    List<User> findByIsCoupleAloneTrue();
    List<User> findByIsMobilityImpairedTrue();
    List<User> findByIsRelianceEquipmentTrue();
    List<User> findByIsChronicDiseaseTrue();
    List<User> findByIsRelianceEnvironmentTrue();
    List<User> findByIsProductionTrue();

    
    List<User> findByUserType(String userType);

    
    List<User> findByIsActiveTrue();
    
    
    List<User> findByName(String name);
    
    
    long countByIsActiveTrue();
    
    
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // 获取所有不重复的社区列表
    @Query("SELECT DISTINCT u.community FROM User u WHERE u.community IS NOT NULL AND u.community != '' ORDER BY u.community")
    List<String> findDistinctCommunities();

}