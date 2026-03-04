package com.powermanage.enums;


public enum SpecialUserType {
    SINGLE_ELDERLY("单人独居老人", "独居老人，无他人同住"),
    DOUBLE_ELDERLY("双人独居老人", "两位老人共同居住"),
    MOBILITY_IMPAIRED("行动不便老人", "行动能力受限的老人"),
    MEDICAL_DEVICE_DEPENDENT("医疗设备依赖用户", "依赖医疗设备维持生活的用户"),
    RECOVERY_CHRONIC_PATIENT("康复/慢病患者", "处于康复期或患有慢性病的用户"),
    ENVIRONMENT_DEPENDENT("环境依赖用户", "对环境条件有特殊要求的用户"),
    SMALL_PRODUCTION_USER("小型生产用户", "从事小型生产活动的用户");

    private final String name;
    private final String description;

    SpecialUserType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}