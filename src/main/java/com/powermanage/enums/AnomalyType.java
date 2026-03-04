package com.powermanage.enums;


public enum AnomalyType {
    POWER_OUTAGE("瞬时断电", "用电突然中断后恢复"),
    SUDDEN_INCREASE("功率突增", "功率在短时间内急剧增加"),
    SUDDEN_DECREASE("功率突降", "功率在短时间内急剧下降");

    private final String name;
    private final String description;

    AnomalyType(String name, String description) {
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