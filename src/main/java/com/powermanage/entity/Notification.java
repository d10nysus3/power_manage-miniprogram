package com.powermanage.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String recipient; 

    @Column(length = 200)
    private String title; 

    @Column(columnDefinition = "TEXT")
    private String content; 

    @Column(length = 20)
    private String category = "system"; 

    private Boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}


