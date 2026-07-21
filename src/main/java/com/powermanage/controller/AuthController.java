package com.powermanage.controller;

import com.powermanage.entity.SystemUser;
import com.powermanage.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SystemUserRepository systemUserRepository;
    
    // 添加密码编码器
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    
    private String issueToken(String username) {
        return Base64.getEncoder().encodeToString((username + ":token").getBytes(StandardCharsets.UTF_8));
    }

    private String parseUsername(String token) {
        try {
            String raw = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            if (raw.endsWith(":token")) {
                return raw.substring(0, raw.length() - 6);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码为空"));
        }
        
        try {
            Optional<SystemUser> userOpt = systemUserRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(401).body(Map.of("error", "账号不存在"));
            }
            SystemUser u = userOpt.get();
            
            
            // 兼容旧密码和新密码验证逻辑
            boolean passwordValid = false;
            String storedPassword = u.getPasswordHash();
            
            // 尝试使用BCrypt验证（新密码格式）
            if (storedPassword.startsWith("$2a$")) {
                passwordValid = passwordEncoder.matches(password, storedPassword);
            } else {
                // 兼容旧的明文密码验证
                passwordValid = password.equals(storedPassword);
                // 如果明文密码验证成功，更新为加密密码
                if (passwordValid) {
                    u.setPasswordHash(passwordEncoder.encode(password));
                    systemUserRepository.save(u);
                }
            }
            
            if (!passwordValid) {
                return ResponseEntity.status(401).body(Map.of("error", "密码错误"));
            }
            String token = issueToken(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", username);
            response.put("role", u.getRole());
            response.put("userId", u.getId());
            response.put("managedCommunity", u.getManagedCommunity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "登录过程中发生错误: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        String token = auth.substring(7);
        String username = parseUsername(token);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "无效token"));
        }
        Optional<SystemUser> userOpt = systemUserRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
        }
        SystemUser u = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("username", u.getUsername());
        response.put("role", u.getRole());
        response.put("managedCommunity", u.getManagedCommunity());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            // 验证请求参数
            String username = payload.get("username");
            String password = payload.get("password");
            String role = payload.get("role");
            String managedCommunity = payload.get("managedCommunity");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空", "message", "用户名不能为空"));
            }
            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空", "message", "密码不能为空"));
            }
            if (role == null || (!role.equals("admin") && !role.equals("normal"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的角色", "message", "请选择有效的角色"));
            }

            // 检查用户名是否已存在
            Optional<SystemUser> existingUser = systemUserRepository.findByUsername(username);
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "用户名已存在", "message", "用户名已存在，请选择其他用户名"));
            }

            // 创建新用户
            SystemUser newUser = new SystemUser();
            newUser.setUsername(username);
            // 使用密码编码器加密存储密码
            newUser.setPasswordHash(passwordEncoder.encode(password));
            // 将角色值转换为大写，并将'normal'映射为'MANAGER'
            String normalizedRole = role.toUpperCase();
            if ("NORMAL".equals(normalizedRole)) {
                normalizedRole = "MANAGER";
            }
            newUser.setRole(normalizedRole);
            if ("MANAGER".equals(normalizedRole)) {
                if (managedCommunity == null || managedCommunity.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "区管理员必须选择负责社区",
                            "message", "区管理员必须选择负责社区"));
                }
                newUser.setManagedCommunity(managedCommunity.trim());
            }


            // 保存用户到数据库
            SystemUser savedUser = systemUserRepository.save(newUser);

            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("message", "注册成功");
            response.put("userId", savedUser.getId());
            response.put("username", savedUser.getUsername());
            response.put("role", savedUser.getRole());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "注册过程中发生错误", "message", "注册失败，请稍后重试"));
        }
    }
}


