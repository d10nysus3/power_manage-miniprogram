package com.powermanage.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_electricity_data")
public class ElectricityData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime recordTime; 

    private Double electricityConsumption; 
    private Double power; 

    @Column(length = 50)
    private String dataSource; 

    @Column(length = 20)
    private String dataType; 

    private Boolean isValid = true; 
    
    private Integer intervalMinutes = 15; 
    
    
    public Double getElectricityValue() {
        return this.electricityConsumption;
    }
    
    
    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }
}