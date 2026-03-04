package com.powermanage.controller;

import com.powermanage.entity.SystemUser;
import com.powermanage.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/system-users")
public class SystemUserController {

    @Autowired
    private SystemUserRepository systemUserRepository;

    
    @GetMapping
    public ResponseEntity<java.util.Map<String, Object>> getAllSystemUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        List<SystemUser> users = systemUserRepository.findAll().stream()
                .filter(user -> {
                    boolean match = true;
                    if (activeOnly && !user.isActive()) {
                        match = false;
                    }
                    if (role != null && !role.isEmpty() && !role.equals(user.getRole())) {
                        match = false;
                    }
                    if (keyword != null && !keyword.isEmpty()) {
                        String searchTerm = keyword.toLowerCase();
                        if (!String.valueOf(user.getId()).contains(searchTerm) && 
                            !user.getUsername().toLowerCase().contains(searchTerm)) {
                            match = false;
                        }
                    }
                    return match;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // 计算总数
        long total = users.size();
        
        // 实现分页
        int start = Math.min((page - 1) * pageSize, (int)total);
        int end = Math.min(start + pageSize, (int)total);
        List<SystemUser> pageUsers = start < end ? users.subList(start, end) : new java.util.ArrayList<>();
        
        // 构建返回结果
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("records", pageUsers);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);
        
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<SystemUser> getSystemUserById(@PathVariable Long id) {
        Optional<SystemUser> user = systemUserRepository.findById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
    @PostMapping
    public ResponseEntity<SystemUser> createSystemUser(@RequestBody SystemUser user) {
        
        if (systemUserRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        SystemUser savedUser = systemUserRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<SystemUser> updateSystemUser(@PathVariable Long id, @RequestBody SystemUser user) {
        if (!systemUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        SystemUser updatedUser = systemUserRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSystemUser(@PathVariable Long id) {
        if (!systemUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        systemUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}