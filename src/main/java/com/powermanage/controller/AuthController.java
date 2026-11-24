package com.powermanage.controller;

import com.powermanage.entity.SystemUser;
import com.powermanage.repository.SystemUserRepository;
import com.powermanage.service.LoginSessionService;
import com.powermanage.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SystemUserRepository users;
    private final LoginSessionService sessions;
    private final AuditService audit;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AuthController(SystemUserRepository users, LoginSessionService sessions, AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.audit = audit;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || username.isBlank() || username.length() > 64 || password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码无效"));
        Optional<SystemUser> found = users.findByUsername(username.trim());
        if (found.isEmpty()) {
            audit.logAction(username.trim(), "LOGIN_FAILED");
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
        SystemUser user = found.get();
        if (!user.isActive()) return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
            return ResponseEntity.status(429).body(Map.of("error", "登录失败次数过多，请稍后重试"));
        String stored = user.getPasswordHash();
        boolean bcrypt = stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
        boolean matches = stored != null && (bcrypt ? passwords.matches(password, stored) : password.equals(stored));
        if (!matches) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures >= 5 ? 0 : failures);
            if (failures >= 5) user.setLockedUntil(now.plusMinutes(15));
            users.save(user);
            audit.logAction(user.getUsername(), "LOGIN_FAILED");
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
        if (!bcrypt) user.setPasswordHash(passwords.encode(password));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        users.save(user);
        String token = sessions.create(user);
        audit.logAction(user.getUsername(), "LOGIN_SUCCESS");
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("userId", user.getId());
        response.put("managedCommunity", user.getManagedCommunity());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        return users.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(Map.of("username", user.getUsername(),
                        "role", user.getRole(), "managedCommunity",
                        user.getManagedCommunity() == null ? "" : user.getManagedCommunity())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) sessions.revoke(authorization.substring(7));
        if (authentication != null) audit.logAction(authentication.getName(), "LOGOUT");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String role = payload.get("role");
        String community = payload.get("managedCommunity");
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,64}") ||
                password == null || password.length() < 12 || password.length() > 128 ||
                role == null || !(role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("normal") || role.equalsIgnoreCase("manager")))
            return ResponseEntity.badRequest().body(Map.of("error", "注册参数无效"));
        String normalizedRole = role.equalsIgnoreCase("admin") ? "ADMIN" : "MANAGER";
        if ("MANAGER".equals(normalizedRole) && (community == null || community.isBlank() || community.length() > 100))
            return ResponseEntity.badRequest().body(Map.of("error", "请选择负责社区"));
        if (users.findByUsername(username).isPresent())
            return ResponseEntity.status(409).body(Map.of("error", "用户名已存在"));
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setPasswordHash(passwords.encode(password));
        user.setRole(normalizedRole);
        user.setManagedCommunity("MANAGER".equals(normalizedRole) ? community.trim() : null);
        user.setActive(true);
        user.setCreateTime(LocalDateTime.now());
        SystemUser saved = users.save(user);
        return ResponseEntity.status(201).body(Map.of("message", "创建成功", "userId", saved.getId(),
                "username", saved.getUsername(), "role", saved.getRole()));
    }
}
