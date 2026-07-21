package com.powermanage.controller;

import com.powermanage.entity.User;
import com.powermanage.entity.SystemUser;
import com.powermanage.service.UserAccessService;
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

    @Autowired
    private UserAccessService userAccessService;

    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        userAccessService.applyCommunityForCreate(currentUser, user);
        User savedUser = userService.saveUser(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        Optional<User> user = userService.getUserById(id);
        return user.filter(value -> userAccessService.canAccess(currentUser, value))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @GetMapping("/idcard/{idCard}")
    public ResponseEntity<User> getUserByIdCard(@PathVariable String idCard) {
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        Optional<User> user = userService.getUserByIdCard(idCard);
        return user.filter(value -> userAccessService.canAccess(currentUser, value))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @GetMapping("/meternumber/{meterNumber}")
    public ResponseEntity<User> getUserByMeterNumber(@PathVariable String meterNumber) {
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        Optional<User> user = userService.getUserByMeterNumber(meterNumber);
        return user.filter(value -> userAccessService.canAccess(currentUser, value))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        List<User> users = userAccessService.filterAccessible(currentUser, userService.getAllUsers());
        if (community != null && !community.isEmpty()) {
            users = users.stream()
                    .filter(user -> community.equals(user.getCommunity()))
                    .collect(java.util.stream.Collectors.toList());
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
        return getAccessibleUsers(userService.getUsersByCommunity(community));
    }

    
    @GetMapping("/special/single-alone")
    public List<User> getSingleAloneUsers() {
        return getAccessibleUsers(userService.getSingleAloneUsers());
    }

    @GetMapping("/special/couple-alone")
    public List<User> getCoupleAloneUsers() {
        return getAccessibleUsers(userService.getCoupleAloneUsers());
    }

    @GetMapping("/special/mobility-impaired")
    public List<User> getMobilityImpairedUsers() {
        return getAccessibleUsers(userService.getMobilityImpairedUsers());
    }

    @GetMapping("/special/reliance-equipment")
    public List<User> getRelianceEquipmentUsers() {
        return getAccessibleUsers(userService.getRelianceEquipmentUsers());
    }

    @GetMapping("/special/chronic-disease")
    public List<User> getChronicDiseaseUsers() {
        return getAccessibleUsers(userService.getChronicDiseaseUsers());
    }

    @GetMapping("/special/reliance-environment")
    public List<User> getRelianceEnvironmentUsers() {
        return getAccessibleUsers(userService.getRelianceEnvironmentUsers());
    }

    @GetMapping("/special/production")
    public List<User> getProductionUsers() {
        return getAccessibleUsers(userService.getProductionUsers());
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        Optional<User> existingUser = userService.getUserById(id);
        if (!existingUser.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        userAccessService.applyCommunityForUpdate(currentUser, existingUser.get(), user);
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
        Optional<User> existingUser = userService.getUserById(id);
        if (!existingUser.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userAccessService.assertCanAccess(userAccessService.getCurrentSystemUser(), existingUser.get());
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    
    @GetMapping("/type/{userType}")
    public List<User> getUsersByType(@PathVariable String userType) {
        return getAccessibleUsers(userService.getUsersByType(userType));
    }

    
    @GetMapping("/active")
    public List<User> getActiveUsers() {
        return getAccessibleUsers(userService.getActiveUsers());
    }
    
    // 获取所有不重复的社区列表
    @GetMapping("/communities")
    public List<String> getCommunities() {
        SystemUser currentUser = userAccessService.getCurrentSystemUser();
        if (userAccessService.isAdmin(currentUser)) {
            return userService.getAllCommunities();
        }
        return java.util.Collections.singletonList(userAccessService.requireManagedCommunity(currentUser));
    }

    private List<User> getAccessibleUsers(List<User> users) {
        return userAccessService.filterAccessible(userAccessService.getCurrentSystemUser(), users);
    }
}
