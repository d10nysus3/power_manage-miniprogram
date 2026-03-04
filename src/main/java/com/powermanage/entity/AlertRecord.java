package com.powermanage.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_alert_record")
public class AlertRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "detection_record_id")
    private AnomalyDetectionRecord detectionRecord;

    @Column(nullable = false)
    private LocalDateTime alertTime; 

    @Column(length = 50, nullable = false)
    private String alertType; 

    @Column(length = 20)
    private String severityLevel; 

    private String triggerReason; 
    private String alertContent; 
    private Boolean isProcessed = false; 
    private Boolean isRead = false; 
    private Boolean isFalseAlarm = false; 
    private LocalDateTime processTime; 
    private String processor; 
    
    
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    
    public String getStatus() {
        return isProcessed ? "已处理" : "待处理";
    }
    
    
    public String getProcessResult() {
        return isProcessed ? "处理完成" : "未处理";
    }
}