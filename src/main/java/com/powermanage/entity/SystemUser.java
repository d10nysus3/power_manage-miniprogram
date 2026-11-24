package com.powermanage.entity;

import lombok.Data;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
@Entity
@Table(name = "t_system_user")  
public class SystemUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 255)  
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias("password")
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String role;

    /**
     * Community whose resident records this manager may access.
     * ADMIN users are not restricted by this field.
     */
    @Column(name = "managed_community", length = 100)
    private String managedCommunity;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;

    @Column(name = "is_active")  
    private boolean active;

    @Column(name = "create_time")  
    private java.time.LocalDateTime createTime;

    @Column(name = "update_time")  
    private java.time.LocalDateTime updateTime;
}


