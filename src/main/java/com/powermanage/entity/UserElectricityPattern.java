package com.powermanage.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "t_user_electricity_pattern")
public class UserElectricityPattern {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(columnDefinition = "JSON")
    private String patternData; 

    @Column(columnDefinition = "JSON")
    private String featureVector; 

    @ElementCollection
    @CollectionTable(name = "t_user_electricity_tags", joinColumns = @JoinColumn(name = "pattern_id"))
    @Column(name = "tag_name")
    private List<String> featureTags; 

    private LocalDate lastAnalysisDate; 
    private LocalDateTime analysisDate; 
    private String analysisAlgorithm; 
    private String algorithm; 
    private Integer analysisVersion = 1; 
    private String version = "1.0"; 
    private String description; 
    private String dataPeriod; 
    private String patternType; 
    private Double confidence; 
    
    
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    
    public void setUserId(Long userId) {
        if (this.user == null) {
            this.user = new User();
        }
        this.user.setId(userId);
    }
    
    
    public String getPatternData() {
        return patternData;
    }
    
    
    public void setPatternData(String patternData) {
        this.patternData = patternData;
    }
    
    
    public String getVersion() {
        return version;
    }
    
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    
    public String getDataPeriod() {
        return dataPeriod;
    }
    
    public void setDataPeriod(String dataPeriod) {
        this.dataPeriod = dataPeriod;
    }
    
    
    public String getPatternType() {
        return patternType;
    }
    
    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }
    
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}