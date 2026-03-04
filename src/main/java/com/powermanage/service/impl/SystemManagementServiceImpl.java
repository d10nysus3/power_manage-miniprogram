package com.powermanage.service.impl;



import com.powermanage.entity.*;

import com.powermanage.repository.*;

import com.powermanage.service.SystemManagementService;

import com.powermanage.utils.ResponseUtil;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import java.io.PrintWriter;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.List;

import java.util.Optional;

import java.util.Random;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.Set;



@Service

public class SystemManagementServiceImpl implements SystemManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SystemManagementServiceImpl.class);

    

    @Autowired

    private SystemUserRepository systemUserRepository;

    

    @Autowired

    private UserRepository userRepository;

    

    @Autowired

    private ElectricityDataRepository electricityDataRepository;

    

    @Autowired

    private AnomalyDetectionRecordRepository anomalyDetectionRecordRepository;

    

    @Autowired

    private AlertRecordRepository alertRecordRepository;

    

    @Autowired

    private AlertProcessRecordRepository alertProcessRecordRepository;

    

    @Autowired

    private UserElectricityPatternRepository electricityPatternRepository;

    

    @Autowired

    private SettingRepository settingRepository;

    

    @Autowired

    private AuditLogRepository auditLogRepository;

    

    @Autowired

    private NotificationRepository notificationRepository;

    

    

    @Override

    @Transactional

    public void clearAllData() {

        logger.info("开始清空所有数据");

        

        

        alertProcessRecordRepository.deleteAll();

        alertRecordRepository.deleteAll();

        anomalyDetectionRecordRepository.deleteAll();

        electricityPatternRepository.deleteAll();

        electricityDataRepository.deleteAll();

        userRepository.deleteAll();

        

        

        Optional<SystemUser> adminUser = systemUserRepository.findByUsername("admin");

        if (adminUser.isPresent()) {

            

            SystemUser admin = adminUser.get();

            String username = admin.getUsername();

            String passwordHash = admin.getPasswordHash();

            String role = admin.getRole();

            LocalDateTime createTime = admin.getCreateTime();

            LocalDateTime updateTime = admin.getUpdateTime();

            Boolean isActive = admin.isActive();

            

            

            List<SystemUser> allUsers = systemUserRepository.findAll();

            for (SystemUser user : allUsers) {

                if (!"admin".equals(user.getUsername())) {

                    systemUserRepository.delete(user);

                }

            }

            

            

            admin.setPasswordHash(passwordHash);

            admin.setRole(role);

            admin.setCreateTime(createTime);

            admin.setUpdateTime(updateTime);

            admin.setActive(isActive);

            

            systemUserRepository.save(admin);

            logger.info("已保留admin账号");

        } else {

            

            systemUserRepository.deleteAll();

        }

        

        settingRepository.deleteAll();

        auditLogRepository.deleteAll();

        notificationRepository.deleteAll();

        

        logger.info("所有数据已清空，admin账号已保留");

    }

    

    

    @Override

    public void backupData(HttpServletResponse response) {

        try {

            

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String filename = "power_manage_backup_" + timestamp + ".sql";

            

            response.setContentType("application/octet-stream");

            response.setCharacterEncoding("UTF-8");

            

            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);

            

            PrintWriter writer = response.getWriter();

            

            

            writer.println("-- 用电异常监测与管理系统数据备份");

            writer.println("-- 备份时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            writer.println("SET NAMES utf8mb4;");

            writer.println("SET FOREIGN_KEY_CHECKS=0;");

            writer.println();

            

            

            backupSystemUsers(writer);

            

            

            backupUsers(writer);

            

            

            backupElectricityData(writer);

            

            

            backupAnomalyRecords(writer);

            

            

            backupAlertRecords(writer);

            

            

            backupAlertProcessRecords(writer);

            

            

            backupElectricityPatterns(writer);

            

            

            backupSettings(writer);

            

            writer.println("SET FOREIGN_KEY_CHECKS=1;");

            writer.flush();

        } catch (IOException e) {

            logger.error("备份数据失败: {}", e.getMessage(), e);

        }

    }

    

    

    @Override

    @Transactional

    public void generateRandomData() {

        logger.info("开始生成随机数据");

        

        Random random = new Random();

        

        

        generateSystemUsers(random);

        

        

        generateUsers(random);

        

        

        generateElectricityData(random);

        

        

        generateAnomalyRecords(random);

        

        

        generateAlertRecords(random);

        

        

        generateAlertProcessRecords(random);

        

        

        generateUserElectricityPatterns(random);

        

        

        generateSettings(random);

        

        

        generateAuditLogs(random);

        

        

        generateNotifications(random);

        

        logger.info("随机数据生成完成");

    }

    

    

    

    private void backupSystemUsers(PrintWriter writer) {

        writer.println("-- 系统用户数据");

        List<SystemUser> systemUsers = systemUserRepository.findAll();

        for (SystemUser user : systemUsers) {

            

            if (!"admin".equals(user.getUsername())) {

                writer.printf("INSERT INTO t_system_user (id, username, password_hash, role, create_time, update_time, is_active) VALUES (%d, '%s', '%s', '%s', '%s', '%s', %b);\n",

                        user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole(), 

                        user.getCreateTime(), user.getUpdateTime(), user.isActive());

            }

        }

        writer.println();

    }

    

    private void backupUsers(PrintWriter writer) {

        writer.println("-- 用户数据");

        List<User> users = userRepository.findAll();

        for (User user : users) {

            writer.printf("INSERT INTO t_user (id, id_card, name, gender, birth_date, phone_number, address, emergency_contact_name, emergency_contact_phone, is_alone, has_chronic_disease, has_oxygen_machine, has_dialysis_machine, has_ventilator, has_other_devices, device_usage_notes, power_supply_area, community, meter_number, community_manager_name, community_manager_phone, user_type, is_active, create_time) VALUES (%d, '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', %b, %b, %b, %b, %b, %b, '%s', '%s', '%s', '%s', '%s', '%s', '%s', %b, '%s');\n",

                    user.getId(), user.getIdCard(), user.getName(), user.getGender(), user.getBirthDate(), 

                    user.getPhoneNumber(), user.getAddress(), user.getEmergencyContactName(), user.getEmergencyContactPhone(),

                    user.getIsSingleAlone(), user.getIsCoupleAlone(), user.getIsMobilityImpaired(), user.getIsRelianceEquipment(), 

                    user.getIsChronicDisease(), user.getIsRelianceEnvironment(), user.getIsProduction(), user.getDeviceUsageNotes(), user.getPowerSupplyArea(), 

                    user.getCommunity(), user.getMeterNumber(), user.getCommunityManagerName(), user.getCommunityManagerPhone(), 

                    user.getUserType(), user.getIsActive(), user.getCreateTime());

        }

        writer.println();

    }

    

    private void backupElectricityData(PrintWriter writer) {

        writer.println("-- 用电数据");

        List<ElectricityData> data = electricityDataRepository.findAll();

        for (ElectricityData item : data) {

            writer.printf("INSERT INTO t_electricity_data (id, user_id, record_time, electricity_consumption, power, data_source, data_type, is_valid, interval_minutes) VALUES (%d, %d, '%s', %f, %f, '%s', '%s', %b, %d);\n",

                    item.getId(), item.getUserId(), item.getRecordTime(), item.getElectricityConsumption(), 

                    item.getPower(), item.getDataSource(), item.getDataType(), item.getIsValid(), item.getIntervalMinutes());

        }

        writer.println();

    }

    

    private void backupAnomalyRecords(PrintWriter writer) {

        writer.println("-- 异常检测记录");

        List<AnomalyDetectionRecord> records = anomalyDetectionRecordRepository.findAll();

        for (AnomalyDetectionRecord record : records) {

            writer.printf("INSERT INTO t_anomaly_detection_record (id, user_id, detection_data_id, detection_time, anomaly_type, severity_level, detection_params, detection_result, anomaly_score, is_anomaly, algorithm, algorithm_version, analysis_notes, anomaly_details, is_handled) VALUES (%d, %d, %d, '%s', '%s', '%s', '%s', '%s', %f, %b, '%s', %d, '%s', '%s', %b);\n",

                    record.getId(), record.getUserId(), record.getDetectionDataId(), record.getDetectionTime(), 

                    record.getAnomalyType(), record.getSeverityLevel(), record.getDetectionParams(), record.getDetectionResult(),

                    record.getAnomalyScore(), record.getIsAnomaly(), record.getAlgorithm(), record.getAlgorithmVersion(),

                    record.getAnalysisNotes(), record.getAnomalyDetails(), record.getIsHandled());

        }

        writer.println();

    }

    

    private void backupAlertRecords(PrintWriter writer) {

        writer.println("-- 告警记录");

        List<AlertRecord> records = alertRecordRepository.findAll();

        for (AlertRecord record : records) {

            

            String processTimeStr = record.getProcessTime() != null ? "'" + record.getProcessTime() + "'" : "NULL";

            String processorStr = record.getProcessor() != null ? "'" + record.getProcessor() + "'" : "NULL";

            

            writer.printf("INSERT INTO t_alert_record (id, user_id, detection_record_id, alert_time, alert_type, severity_level, trigger_reason, alert_content, is_processed, process_time, processor) VALUES (%d, %d, %d, '%s', '%s', '%s', '%s', '%s', %b, %s, %s);\n",

                    record.getId(), record.getUser().getId(), record.getDetectionRecord().getId(), record.getAlertTime(),

                    record.getAlertType(), record.getSeverityLevel(), record.getTriggerReason(), record.getAlertContent(),

                    record.getIsProcessed(), processTimeStr, processorStr);

        }

        writer.println();

    }

    

    private void backupAlertProcessRecords(PrintWriter writer) {

        writer.println("-- 告警处理记录");

        List<AlertProcessRecord> records = alertProcessRecordRepository.findAll();

        for (AlertProcessRecord record : records) {

            

            String processingTimeStr = record.getProcessingTime() != null ? "'" + record.getProcessingTime() + "'" : "NULL";

            

            writer.printf("INSERT INTO t_alert_process_record (id, alert_id, alert_type, has_safety_hazard, processor, processing_measures, processing_time, processing_result, archive_status, processing_notes) VALUES (%d, %d, '%s', %b, '%s', '%s', %s, '%s', '%s', '%s');\n",

                    record.getId(), record.getAlertRecord().getId(), record.getAlertType(), 

                    record.getHasSafetyHazard(), record.getProcessor(), record.getProcessingMeasures(),

                    processingTimeStr, record.getProcessingResult(), record.getArchiveStatus(), record.getProcessingNotes());

        }

        writer.println();

    }

    

    private void backupElectricityPatterns(PrintWriter writer) {

        writer.println("-- 用电模式数据");

        List<UserElectricityPattern> patterns = electricityPatternRepository.findAll();

        for (UserElectricityPattern pattern : patterns) {

            

            String lastAnalysisDateStr = pattern.getLastAnalysisDate() != null ? "'" + pattern.getLastAnalysisDate() + "'" : "NULL";

            String analysisDateStr = pattern.getAnalysisDate() != null ? "'" + pattern.getAnalysisDate() + "'" : "NULL";

            

            writer.printf("INSERT INTO t_user_electricity_pattern (id, user_id, pattern_data, feature_vector, last_analysis_date, analysis_date, analysis_algorithm, algorithm, analysis_version, version, description) VALUES (%d, %d, '%s', '%s', %s, %s, '%s', '%s', %d, '%s', '%s');\n",

                    pattern.getId(), pattern.getUserId(), pattern.getPatternData(), pattern.getFeatureVector(),

                    lastAnalysisDateStr, analysisDateStr, pattern.getAnalysisAlgorithm(),

                    pattern.getAlgorithm(), pattern.getAnalysisVersion(), pattern.getVersion(), pattern.getDescription());

        }

        writer.println();

    }

    

    private void backupSettings(PrintWriter writer) {

        writer.println("-- 系统设置数据");

        List<Setting> settings = settingRepository.findAll();

        for (Setting setting : settings) {

            writer.printf("INSERT INTO t_setting (`key`, value) VALUES ('%s', '%s');\n",

                    setting.getKey(), setting.getValue());

        }

        writer.println();

    }

    

    

    

    private void generateSystemUsers(Random random) {

        

        Optional<SystemUser> existingAdmin = systemUserRepository.findByUsername("admin");

        if (!existingAdmin.isPresent()) {

            SystemUser admin = new SystemUser();

            admin.setUsername("admin");

            admin.setPasswordHash("123456");

            admin.setRole("ADMIN");

            admin.setActive(true);

            admin.setCreateTime(LocalDateTime.now());

            admin.setUpdateTime(LocalDateTime.now());

            systemUserRepository.save(admin);

            logger.info("已创建admin用户");

        }

        

        

        for (int i = 1; i <= 5; i++) {

            String username = "manager" + i;

            

            Optional<SystemUser> existingUser = systemUserRepository.findByUsername(username);

            if (!existingUser.isPresent()) {

                SystemUser user = new SystemUser();

                user.setUsername(username);

                user.setPasswordHash("123456");

                user.setRole("MANAGER"); 

                user.setActive(true);

                user.setCreateTime(LocalDateTime.now());

                user.setUpdateTime(LocalDateTime.now());

                systemUserRepository.save(user);

            }

        }

    }

    

    private void generateUsers(Random random) {

        String[] genders = {"男", "女"};

        String[] userTypes = {"住宅用户", "商业用户", "工业用户"};

        String[] areas = {"朝阳区供电所", "海淀区供电所", "西城区供电所", "东城区供电所", "丰台区供电所"};

        String[] communities = {"XX社区", "YY社区", "ZZ社区", "AA社区", "BB社区"};

        

        for (int i = 1; i <= 20; i++) {

            User user = new User();

            user.setIdCard("ID" + String.format("%010d", i));

            user.setName("用户" + i);

            user.setGender(genders[random.nextInt(genders.length)]);

            user.setBirthDate(java.time.LocalDate.now().minusYears(20 + random.nextInt(50)));

            user.setPhoneNumber("138" + String.format("%08d", random.nextInt(100000000)));

            user.setAddress("北京市" + areas[random.nextInt(areas.length)] + "小区" + (i % 10 + 1) + "号楼" + (i % 5 + 1) + "单元" + (i % 3 + 1) + "0" + (i % 10 + 1) + "室");

            user.setEmergencyContactName("紧急联系人" + i);

            user.setEmergencyContactPhone("139" + String.format("%08d", random.nextInt(100000000)));

            

            
            int userTypeIndex = random.nextInt(7);

            switch (userTypeIndex) {

                case 0: 
                    user.setIsSingleAlone(true);

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(random.nextBoolean());

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

                case 1: 
                    user.setIsSingleAlone(false);

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(random.nextBoolean());

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

                case 2: 
                    user.setIsSingleAlone(random.nextBoolean());

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(true);

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

                case 3: 
                    user.setIsSingleAlone(random.nextBoolean());

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(random.nextBoolean());

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

                case 4: 
                    user.setIsSingleAlone(random.nextBoolean());

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(true);

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

                case 5: 
                    user.setIsSingleAlone(random.nextBoolean());

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(true);

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

                case 6: 
                    user.setIsSingleAlone(random.nextBoolean());

                    user.setIsCoupleAlone(random.nextBoolean());

                    user.setIsMobilityImpaired(random.nextBoolean());

                    user.setIsRelianceEquipment(random.nextBoolean());

                    user.setIsChronicDisease(false);

                    user.setIsRelianceEnvironment(random.nextBoolean());

                    user.setIsProduction(random.nextBoolean());

                    break;

            }

            

            user.setDeviceUsageNotes("设备使用备注" + i);

            user.setPowerSupplyArea(areas[random.nextInt(areas.length)]);

            user.setCommunity(communities[random.nextInt(communities.length)]);

            user.setMeterNumber("M" + String.format("%09d", random.nextInt(1000000000)));

            user.setCommunityManagerName("社区管理员" + (i % 5 + 1));

            user.setCommunityManagerPhone("137" + String.format("%08d", random.nextInt(100000000)));

            user.setUserType(userTypes[random.nextInt(userTypes.length)]);

            user.setIsActive(true);

            user.setCreateTime(java.time.LocalDate.now().minusDays(random.nextInt(365)));

            userRepository.save(user);

        }

    }

    

    private void generateElectricityData(Random random) {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) return;

        

        

        for (User user : users) {

            for (int i = 0; i < 30; i++) {

                ElectricityData data = new ElectricityData();

                data.setUser(user);

                data.setRecordTime(LocalDateTime.now().minusDays(i).minusHours(random.nextInt(24)));

                data.setElectricityConsumption(1.0 + random.nextDouble() * 10.0);

                data.setPower(0.2 + random.nextDouble() * 3.0);

                data.setDataSource("智能电表");

                data.setDataType("历史");

                data.setIsValid(true);

                data.setIntervalMinutes(15);

                electricityDataRepository.save(data);

            }

            

            

            for (int i = 0; i < 10; i++) {

                ElectricityData data = new ElectricityData();

                data.setUser(user);

                data.setRecordTime(LocalDateTime.now().minusHours(i));

                data.setElectricityConsumption(1.0 + random.nextDouble() * 5.0);

                data.setPower(0.2 + random.nextDouble() * 2.0);

                data.setDataSource("智能电表");

                data.setDataType("实时");

                data.setIsValid(true);

                data.setIntervalMinutes(15);

                electricityDataRepository.save(data);

            }

        }

    }

    

    private void generateAnomalyRecords(Random random) {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) return;

        

        String[] anomalyTypes = {"瞬时断电", "功率突增", "功率突降"};

        String[] severityLevels = {"低", "中", "高"};

        

        for (User user : users) {

            

            int count = 1 + random.nextInt(3);

            for (int i = 0; i < count; i++) {

                AnomalyDetectionRecord record = new AnomalyDetectionRecord();

                record.setUserId(user.getId());

                record.setDetectionDataId((long)(random.nextInt(1000)));

                record.setDetectionTime(LocalDateTime.now().minusDays(random.nextInt(30)));

                record.setAnomalyType(anomalyTypes[random.nextInt(anomalyTypes.length)]);

                record.setSeverityLevel(severityLevels[random.nextInt(severityLevels.length)]);

                record.setDetectionParams("{\"threshold\": 2.0}");

                record.setDetectionResult("{\"detected_value\": 5.2}");

                record.setAnomalyScore(0.5 + random.nextDouble() * 0.5);

                record.setIsAnomaly(true);

                record.setAlgorithm("IsolationForest");

                record.setAlgorithmVersion(1);

                record.setAnalysisNotes("分析备注" + i);

                record.setAnomalyDetails("异常详情" + i);

                record.setIsHandled(random.nextBoolean());

                anomalyDetectionRecordRepository.save(record);

            }

        }

    }

    

    private void generateAlertRecords(Random random) {

        List<AnomalyDetectionRecord> anomalies = anomalyDetectionRecordRepository.findAll();

        if (anomalies.isEmpty()) return;

        

        String[] alertTypes = {"瞬时断电", "功率突增", "功率突降"};

        String[] severityLevels = {"低", "中", "高"};

        String[] processors = {"社区管理员-王五", "社区管理员-孙七", "社区管理员-吴九", "社区管理员-郑十"};

        

        for (AnomalyDetectionRecord anomaly : anomalies) {

            

            if (random.nextDouble() < 0.7) {

                

                Optional<User> userOptional = userRepository.findById(anomaly.getUserId());

                if (!userOptional.isPresent()) {

                    continue;

                }

                

                AlertRecord alert = new AlertRecord();

                alert.setUser(userOptional.get());

                alert.setDetectionRecord(anomaly);

                alert.setAlertTime(anomaly.getDetectionTime().plusMinutes(5));

                alert.setAlertType(alertTypes[random.nextInt(alertTypes.length)]);

                alert.setSeverityLevel(severityLevels[random.nextInt(severityLevels.length)]);

                alert.setTriggerReason("触发原因");

                alert.setAlertContent("告警内容");

                alert.setIsProcessed(random.nextBoolean());

                

                if (alert.getIsProcessed()) {

                    alert.setProcessTime(alert.getAlertTime().plusHours(1 + random.nextInt(24)));

                    alert.setProcessor(processors[random.nextInt(processors.length)]);

                }

                

                alertRecordRepository.save(alert);

            }

        }

    }

    

    private void generateAlertProcessRecords(Random random) {

        List<AlertRecord> alerts = alertRecordRepository.findAll();

        if (alerts.isEmpty()) return;

        

        String[] processors = {"社区管理员-王五", "社区管理员-孙七", "社区管理员-吴九", "社区管理员-郑十"};

        String[] processingMeasures = {"电话通知", "上门检查", "远程指导", "设备维修", "紧急处理"};

        String[] processingResults = {"已解决", "处理中", "待跟进", "已转交", "已完成"};

        String[] archiveStatuses = {"已归档", "待归档", "长期跟踪"};

        

        for (AlertRecord alert : alerts) {

            

            if (random.nextDouble() < 0.8) {

                AlertProcessRecord record = new AlertProcessRecord();

                record.setAlertRecord(alert);

                record.setAlertType(alert.getAlertType());

                record.setHasSafetyHazard(random.nextBoolean());

                record.setProcessor(processors[random.nextInt(processors.length)]);

                record.setProcessingMeasures(processingMeasures[random.nextInt(processingMeasures.length)]);

                record.setProcessingTime(alert.getAlertTime().plusHours(1 + random.nextInt(24)));

                record.setProcessingResult(processingResults[random.nextInt(processingResults.length)]);

                record.setArchiveStatus(archiveStatuses[random.nextInt(archiveStatuses.length)]);

                record.setProcessingNotes("处理备注：" + alert.getAlertType() + "告警处理");

                alertProcessRecordRepository.save(record);

            }

        }

    }

    

    private void generateUserElectricityPatterns(Random random) {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) return;

        

        String[] patternTypes = {"规律型", "波动型", "高峰型", "低谷型", "随机型"};

        String[] algorithms = {"K-means", "DBSCAN", "IsolationForest", "LSTM", "ARIMA"};

        String[] dataPeriods = {"最近7天", "最近30天", "最近90天", "最近半年", "最近一年"};

        

        for (User user : users) {

            

            if (user == null) {

                continue;

            }

            

            UserElectricityPattern pattern = new UserElectricityPattern();

            pattern.setUser(user);

            pattern.setPatternData("{\"peak_time\": \"18:00\", \"peak_consumption\": 5.2, \"valley_time\": \"03:00\", \"valley_consumption\": 0.8}");

            pattern.setFeatureVector("[0.2, 0.5, 0.8, 1.2, 0.9, 0.3, 0.1]");

            pattern.setLastAnalysisDate(java.time.LocalDate.now().minusDays(random.nextInt(30)));

            pattern.setAnalysisDate(LocalDateTime.now().minusHours(random.nextInt(24)));

            pattern.setAnalysisAlgorithm(algorithms[random.nextInt(algorithms.length)]);

            pattern.setAlgorithm(algorithms[random.nextInt(algorithms.length)]);

            pattern.setAnalysisVersion(1);

            pattern.setVersion("1.0");

            pattern.setDescription("用户" + user.getName() + "的用电模式分析");

            pattern.setDataPeriod(dataPeriods[random.nextInt(dataPeriods.length)]);

            pattern.setPatternType(patternTypes[random.nextInt(patternTypes.length)]);

            pattern.setConfidence(0.7 + random.nextDouble() * 0.3);

            

            

            Set<String> tags = new HashSet<>();

            String[] possibleTags = {"高耗能", "规律强劲", "夜间用电", "峰谷明显", "设备集中"};

            int tagCount = 1 + random.nextInt(possibleTags.length);

            while (tags.size() < tagCount && tags.size() < possibleTags.length) {

                tags.add(possibleTags[random.nextInt(possibleTags.length)]);

            }

            pattern.setFeatureTags(new ArrayList<>(tags));

            

            electricityPatternRepository.save(pattern);

        }

    }

    

    private void generateSettings(Random random) {

        

        String[][] systemSettings = {

            {"system.name", "用电异常监测与管理系统"},

            {"system.version", "1.0.0"},

            {"system.contact", "admin@example.com"},

            {"system.phone", "400-123-4567"},

            {"alert.threshold.high", "100"},

            {"alert.threshold.medium", "80"},

            {"alert.threshold.low", "60"},

            {"data.retention.days", "365"},

            {"backup.auto.enabled", "true"},

            {"backup.auto.interval", "7"}

        };

        

        for (String[] setting : systemSettings) {

            Setting s = new Setting();

            s.setKey(setting[0]);

            s.setValue(setting[1]);

            settingRepository.save(s);

        }

    }

    

    private void generateAuditLogs(Random random) {

        List<SystemUser> systemUsers = systemUserRepository.findAll();

        if (systemUsers.isEmpty()) return;

        

        String[] actions = {"登录系统", "查看用户数据", "生成报表", "修改设置", "处理告警", "备份数据", "清空数据"};

        

        

        for (SystemUser user : systemUsers) {

            int logCount = 5 + random.nextInt(10);

            for (int i = 0; i < logCount; i++) {

                AuditLog log = new AuditLog();

                log.setUsername(user.getUsername());

                log.setAction(actions[random.nextInt(actions.length)]);

                log.setTimestamp(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));

                auditLogRepository.save(log);

            }

        }

    }

    

    private void generateNotifications(Random random) {

        List<SystemUser> systemUsers = systemUserRepository.findAll();

        if (systemUsers.isEmpty()) return;

        

        String[] categories = {"system", "notification", "alert", "reminder"};

        String[] titles = {"系统通知", "告警提醒", "数据更新", "维护通知", "安全提醒"};

        

        

        for (SystemUser user : systemUsers) {

            int notificationCount = 3 + random.nextInt(7);

            for (int i = 0; i < notificationCount; i++) {

                Notification notification = new Notification();

                notification.setRecipient(user.getUsername());

                notification.setTitle(titles[random.nextInt(titles.length)]);

                notification.setContent("这是一条关于" + titles[random.nextInt(titles.length)] + "的通知内容，请及时查看和处理。");

                notification.setCategory(categories[random.nextInt(categories.length)]);

                notification.setIsRead(random.nextBoolean());

                notification.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));

                notificationRepository.save(notification);

            }

        }

    }

}