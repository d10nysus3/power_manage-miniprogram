package com.powermanage.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_alert_process_record")
public class AlertProcessRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "alert_id", nullable = false, unique = true)
    private AlertRecord alertRecord;

    @Column(length = 50)
    private String alertType; 

    private Boolean hasSafetyHazard; 

    private Boolean isFalseAlarm = false; 

    @Column(nullable = false)
    private String processor; 

    private String processingMeasures; 

    @Column(nullable = false)
    private LocalDateTime processingTime; 

    private String processingResult; 

    @Column(length = 20)
    private String archiveStatus; 

    private String processingNotes; 
}