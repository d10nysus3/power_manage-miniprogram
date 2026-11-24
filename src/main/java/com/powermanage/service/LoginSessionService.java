package com.powermanage.service;

import com.powermanage.entity.LoginSession;
import com.powermanage.entity.SystemUser;
import com.powermanage.repository.LoginSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class LoginSessionService {
    private final LoginSessionRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final int lifetimeHours;

    public LoginSessionService(LoginSessionRepository repository,
            @Value("${security.session.hours:8}") int lifetimeHours) {
        this.repository = repository;
        this.lifetimeHours = Math.max(1, Math.min(lifetimeHours, 24));
    }

    public String create(SystemUser user) {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        LoginSession session = new LoginSession();
        session.setTokenHash(hash(token));
        session.setUser(user);
        session.setExpiresAt(LocalDateTime.now().plusHours(lifetimeHours));
        repository.save(session);
        return token;
    }

    public Optional<SystemUser> authenticate(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return repository.findByTokenHash(hash(token))
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(LoginSession::getUser)
                .filter(SystemUser::isActive);
    }

    public void revoke(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return;
        repository.findByTokenHash(hash(token)).ifPresent(repository::delete);
    }

    private String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
