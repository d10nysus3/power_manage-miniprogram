package com.powermanage.service.impl;

import com.powermanage.entity.User;
import com.powermanage.repository.UserRepository;
import com.powermanage.service.AuditService;
import com.powermanage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public User saveUser(User user) {
        User savedUser = userRepository.save(user);
        auditService.logAction("admin", "Created user: " + savedUser.getName());
        return savedUser;
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByIdCard(String idCard) {
        return userRepository.findByIdCard(idCard);
    }

    @Override
    public Optional<User> getUserByMeterNumber(String meterNumber) {
        return userRepository.findByMeterNumber(meterNumber);
    }

    @Override
    public List<User> getAllUsers() {
        
        return userRepository.findByIsActiveTrue();
    }

    @Override
    public List<User> getUsersByCommunity(String community) {
        return userRepository.findByCommunity(community);
    }

    @Override
    public List<User> getSingleAloneUsers() {
        return userRepository.findByIsSingleAloneTrue();
    }

    @Override
    public List<User> getCoupleAloneUsers() {
        return userRepository.findByIsCoupleAloneTrue();
    }

    @Override
    public List<User> getMobilityImpairedUsers() {
        return userRepository.findByIsMobilityImpairedTrue();
    }

    @Override
    public List<User> getRelianceEquipmentUsers() {
        return userRepository.findByIsRelianceEquipmentTrue();
    }

    @Override
    public List<User> getChronicDiseaseUsers() {
        return userRepository.findByIsChronicDiseaseTrue();
    }

    @Override
    public List<User> getRelianceEnvironmentUsers() {
        return userRepository.findByIsRelianceEnvironmentTrue();
    }

    @Override
    public List<User> getProductionUsers() {
        return userRepository.findByIsProductionTrue();
    }

    @Override
    public User updateUser(User user) {
        Optional<User> existingUser = userRepository.findById(user.getId());
        if (existingUser.isPresent()) {
            User updatedUser = userRepository.save(user);
            auditService.logAction("admin", "Updated user: " + updatedUser.getName());
            return updatedUser;
        } else {
            throw new RuntimeException("User not found with id: " + user.getId());
        }
    }

    @Override
    public void deleteUser(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            User updatedUser = user.get();
            updatedUser.setIsActive(false);
            userRepository.save(updatedUser);
            auditService.logAction("admin", "Deactivated user: " + updatedUser.getName());
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }
    }

    @Override
    public List<User> getUsersByType(String userType) {
        return userRepository.findByUserType(userType);
    }

    @Override
    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    @Override
    public List<User> getUsersByName(String name) {
        return userRepository.findByName(name);
    }
    
    @Override
    public long getTotalUserCount() {
        
        return userRepository.countByIsActiveTrue();
    }
    
    @Override
    public long getUserCountByDate(LocalDate date) {
        
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        
        System.out.println("查询用户数量，日期: " + date + ", 开始时间: " + startOfDay + ", 结束时间: " + endOfDay);
        
        try {
            long count = userRepository.countByCreateTimeBetween(startOfDay, endOfDay);
            System.out.println("查询结果数量: " + count);
            return count;
        } catch (Exception e) {
            System.err.println("查询用户数量时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public List<String> getAllCommunities() {
        return userRepository.findDistinctCommunities();
    }
}