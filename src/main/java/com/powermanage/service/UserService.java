package com.powermanage.service;

import com.powermanage.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserService {
    
    User saveUser(User user);

    
    Optional<User> getUserById(Long id);

    
    Optional<User> getUserByIdCard(String idCard);

    
    Optional<User> getUserByMeterNumber(String meterNumber);

    
    List<User> getAllUsers();

    
    List<User> getUsersByCommunity(String community);

    
    List<User> getSingleAloneUsers();
    List<User> getCoupleAloneUsers();
    List<User> getMobilityImpairedUsers();
    List<User> getRelianceEquipmentUsers();
    List<User> getChronicDiseaseUsers();
    List<User> getRelianceEnvironmentUsers();
    List<User> getProductionUsers();

    
    User updateUser(User user);

    
    void deleteUser(Long id);

    
    List<User> getUsersByType(String userType);

    
    List<User> getActiveUsers();
    
    
    List<User> getUsersByName(String name);
    
    
    long getTotalUserCount();
    
    
    long getUserCountByDate(LocalDate date);
    
    // 获取所有不重复的社区列表
    List<String> getAllCommunities();
}