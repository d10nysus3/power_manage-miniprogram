package com.powermanage.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_anomaly_detection_record")
public class AnomalyDetectionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Long detectionDataId; 

    @Column(nullable = false)
    private LocalDateTime detectionTime; 
    
    @Column(nullable = false)
    private String anomalyType; 

    @Column(length = 20)
    private String severityLevel; 

    @Column(columnDefinition = "JSON")
    private String detectionParams; 

    @Column(columnDefinition = "JSON")
    private String detectionResult; 

    private Double anomalyScore; 
    private Boolean isAnomaly = false; 
    private String algorithm; 
    private Integer algorithmVersion = 1; 
    private String analysisNotes; 
    private String anomalyDetails; 
    private Boolean isHandled = false; 
    private LocalDateTime processTime; 
    private String processor; 
    private Boolean isFalseAlarm = false; 
    
    
    public String getNotes() {
        return this.analysisNotes;
    }
    
    
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    
    public void setUserId(Long userId) {
        if (this.user == null) {
            this.user = new User();
        }
        this.user.setId(userId);
    }
}