package com.powermanage.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_login_session", indexes = {
        @Index(name = "idx_login_session_token", columnList = "token_hash", unique = true),
        @Index(name = "idx_login_session_expires", columnList = "expires_at")
})
public class LoginSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "system_user_id", nullable = false)
    private SystemUser user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
