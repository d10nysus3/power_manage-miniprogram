package com.powermanage.controller;

import com.powermanage.entity.User;
import com.powermanage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @GetMapping("/idcard/{idCard}")
    public ResponseEntity<User> getUserByIdCard(@PathVariable String idCard) {
        Optional<User> user = userService.getUserByIdCard(idCard);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @GetMapping("/meternumber/{meterNumber}")
    public ResponseEntity<User> getUserByMeterNumber(@PathVariable String meterNumber) {
        Optional<User> user = userService.getUserByMeterNumber(meterNumber);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        List<User> users;
        if (community != null && !community.isEmpty()) {
            users = userService.getUsersByCommunity(community);
        } else {
            users = userService.getAllUsers();
        }
        
        // 根据关键字过滤
        if (keyword != null && !keyword.isEmpty()) {
            String searchTerm = keyword.toLowerCase();
            users = users.stream()
                    .filter(user -> 
                        String.valueOf(user.getId()).contains(searchTerm) ||
                        user.getName().toLowerCase().contains(searchTerm) ||
                        user.getPhoneNumber().contains(searchTerm) ||
                        user.getIdCard().contains(searchTerm) ||
                        user.getMeterNumber().contains(searchTerm)
                    )
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 计算总数
        long total = users.size();
        
        // 实现分页
        int start = Math.min((page - 1) * pageSize, (int)total);
        int end = Math.min(start + pageSize, (int)total);
        List<User> pageUsers = users.subList(start, end);
        
        // 构建返回结果
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("records", pageUsers);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);
        
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/community/{community}")
    public List<User> getUsersByCommunity(@PathVariable String community) {
        return userService.getUsersByCommunity(community);
    }

    
    @GetMapping("/special/single-alone")
    public List<User> getSingleAloneUsers() {
        return userService.getSingleAloneUsers();
    }

    @GetMapping("/special/couple-alone")
    public List<User> getCoupleAloneUsers() {
        return userService.getCoupleAloneUsers();
    }

    @GetMapping("/special/mobility-impaired")
    public List<User> getMobilityImpairedUsers() {
        return userService.getMobilityImpairedUsers();
    }

    @GetMapping("/special/reliance-equipment")
    public List<User> getRelianceEquipmentUsers() {
        return userService.getRelianceEquipmentUsers();
    }

    @GetMapping("/special/chronic-disease")
    public List<User> getChronicDiseaseUsers() {
        return userService.getChronicDiseaseUsers();
    }

    @GetMapping("/special/reliance-environment")
    public List<User> getRelianceEnvironmentUsers() {
        return userService.getRelianceEnvironmentUsers();
    }

    @GetMapping("/special/production")
    public List<User> getProductionUsers() {
        return userService.getProductionUsers();
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        try {
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    
    @GetMapping("/type/{userType}")
    public List<User> getUsersByType(@PathVariable String userType) {
        return userService.getUsersByType(userType);
    }

    
    @GetMapping("/active")
    public List<User> getActiveUsers() {
        return userService.getActiveUsers();
    }
    
    // 获取所有不重复的社区列表
    @GetMapping("/communities")
    public List<String> getCommunities() {
        return userService.getAllCommunities();
    }
}