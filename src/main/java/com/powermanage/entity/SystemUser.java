package com.powermanage.entity;

import lombok.Data;

import javax.persistence.*;

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
    private String passwordHash;

    @Column(nullable = false, length = 50)  
    private String role;

    @Column(name = "is_active")  
    private boolean active;

    @Column(name = "create_time")  
    private java.time.LocalDateTime createTime;

    @Column(name = "update_time")  
    private java.time.LocalDateTime updateTime;
}


