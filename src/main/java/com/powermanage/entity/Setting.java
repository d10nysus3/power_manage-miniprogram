package com.powermanage.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

@Data
@Entity
@Table(name = "t_setting")
public class Setting {

    @Id
    @Column(name = "`key`") 
    private String key;

    private String value;

    
}