package com.powermanage.entity;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Index;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "t_user", indexes = @Index(name = "idx_community", columnList = "community"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(unique = true, nullable = false)
    private String idCard; 

    @Column(nullable = false)
    private String name; 

    private String gender; 

    private LocalDate birthDate; 

    private String phoneNumber; 

    private String address; 

    private String emergencyContactName; 

    private String emergencyContactPhone; 

    
    private Boolean isSingleAlone; // 单人独居老人
    private Boolean isCoupleAlone; // 双人独居老人
    private Boolean isMobilityImpaired; // 行动不便老人
    private Boolean isRelianceEquipment; // 医疗设备依赖
    private Boolean isChronicDisease; // 康复/慢病
    private Boolean isRelianceEnvironment; // 环境依赖
    private Boolean isProduction; // 小型生产用户

    private String deviceUsageNotes; 

    
    private String userType; 

    
    private String powerSupplyArea; 
    private String community; 
    @Column(unique = true, nullable = false)
    private String meterNumber; 

    
    private String communityManagerName; 
    private String communityManagerPhone; 

    
    private Boolean isActive = true; 
    private LocalDate createTime = LocalDate.now(); 
}