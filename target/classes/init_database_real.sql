
CREATE DATABASE IF NOT EXISTS power_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE power_manage;


CREATE TABLE IF NOT EXISTS t_system_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    role VARCHAR(50) NOT NULL COMMENT '角色',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


INSERT INTO t_system_user (username, password_hash, role)
VALUES ('admin', '123456', 'ADMIN');


CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_card VARCHAR(20) NOT NULL UNIQUE COMMENT '身份证号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender VARCHAR(10) COMMENT '性别',
    birth_date DATE COMMENT '出生日期',
    phone_number VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(255) COMMENT '家庭住址（小区/楼栋/门牌号）',
    emergency_contact_name VARCHAR(50) COMMENT '紧急联系姓名',
    emergency_contact_phone VARCHAR(20) COMMENT '紧急联系电话',
    is_single_alone BOOLEAN COMMENT '单人独居老人标识',
    is_couple_alone BOOLEAN COMMENT '双人独居老人标识',
    is_mobility_impaired BOOLEAN COMMENT '行动不便老人标识',
    is_reliance_equipment BOOLEAN COMMENT '医疗设备依赖标识',
    is_chronic_disease BOOLEAN COMMENT '康复/慢病标识',
    is_reliance_environment BOOLEAN COMMENT '环境依赖标识',
    is_production BOOLEAN COMMENT '小型生产用户标识',
    device_usage_notes TEXT COMMENT '设备使用习惯备注',
    power_supply_area VARCHAR(100) COMMENT '供电片区',
    community VARCHAR(100) COMMENT '社区归属',
    meter_number VARCHAR(50) NOT NULL UNIQUE COMMENT '电表号',
    community_manager_name VARCHAR(50) COMMENT '社区责任人姓名',
    community_manager_phone VARCHAR(20) COMMENT '社区责任人电话',
    user_type VARCHAR(50) COMMENT '用户类型（住宅用户、商业用户、工业用户）',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    create_time DATE DEFAULT (CURRENT_DATE) COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS t_electricity_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    electricity_consumption DOUBLE COMMENT '电量（kWh）',
    power DOUBLE COMMENT '功率（kW）',
    data_source VARCHAR(50) COMMENT '数据来源',
    data_type VARCHAR(20) COMMENT '数据类型（历史/实时）',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '数据是否有效',
    interval_minutes INT DEFAULT 15 COMMENT '数据间隔（分钟）',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS t_anomaly_detection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    detection_data_id BIGINT NOT NULL COMMENT '检测数据ID',
    detection_time DATETIME NOT NULL COMMENT '检测时间',
    anomaly_type VARCHAR(100) NOT NULL COMMENT '异常类型',
    severity_level VARCHAR(20) COMMENT '严重程度（低/中/高）',
    detection_params JSON COMMENT '检测参数',
    detection_result JSON COMMENT '检测结果',
    anomaly_score DOUBLE COMMENT '异常分数',
    is_anomaly BOOLEAN DEFAULT FALSE COMMENT '是否异常',
    algorithm VARCHAR(100) COMMENT '算法名称',
    algorithm_version INT DEFAULT 1 COMMENT '算法版本',
    analysis_notes TEXT COMMENT '分析备注',
    anomaly_details TEXT COMMENT '异常详情',
    is_handled BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
    is_false_alarm BOOLEAN DEFAULT FALSE COMMENT '是否为误报',
    process_time DATETIME COMMENT '处理时间',
    processor VARCHAR(50) COMMENT '处理人',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS t_alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    detection_record_id BIGINT COMMENT '检测记录ID',
    alert_time DATETIME NOT NULL COMMENT '告警时间',
    alert_type VARCHAR(50) NOT NULL COMMENT '异常类型',
    severity_level VARCHAR(20) COMMENT '严重程度（低/中/高）',
    trigger_reason TEXT COMMENT '触发原因',
    alert_content TEXT COMMENT '告警内容',
    is_processed BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
    process_time DATETIME COMMENT '处理时间',
    processor VARCHAR(50) COMMENT '处理人',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    is_false_alarm BOOLEAN DEFAULT FALSE COMMENT '是否为误报',
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (detection_record_id) REFERENCES t_anomaly_detection_record(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS t_alert_process_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL UNIQUE COMMENT '告警ID',
    alert_type VARCHAR(50) COMMENT '告警类型',
    has_safety_hazard BOOLEAN COMMENT '是否存在安全隐患',
    is_false_alarm BOOLEAN DEFAULT FALSE COMMENT '是否为误报',
    processor VARCHAR(50) NOT NULL COMMENT '事件处理人',
    processing_measures TEXT COMMENT '处理措施',
    processing_time DATETIME NOT NULL COMMENT '处理时间',
    processing_result TEXT COMMENT '处理结果',
    archive_status VARCHAR(20) COMMENT '归档状态（已确认/已处理/已归档）',
    processing_notes TEXT COMMENT '处理备注',
    FOREIGN KEY (alert_id) REFERENCES t_alert_record(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS t_user_electricity_pattern (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    pattern_data JSON COMMENT '模式数据(JSON格式)',
    feature_vector JSON COMMENT '用电特征向量(JSON格式)',
    last_analysis_date DATE COMMENT '上次分析日期',
    analysis_date DATETIME COMMENT '分析日期',
    analysis_algorithm VARCHAR(100) COMMENT '分析算法',
    algorithm VARCHAR(100) COMMENT '算法名称',
    analysis_version INT DEFAULT 1 COMMENT '分析版本',
    version VARCHAR(20) DEFAULT '1.0' COMMENT '版本号',
    description TEXT COMMENT '描述',
    data_period VARCHAR(50) COMMENT '数据周期',
    pattern_type VARCHAR(50) COMMENT '模式类型',
    confidence DOUBLE COMMENT '置信度',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS t_user_electricity_tags (
    pattern_id BIGINT NOT NULL COMMENT '模式ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    PRIMARY KEY (pattern_id, tag_name),
    FOREIGN KEY (pattern_id) REFERENCES t_user_electricity_pattern(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE IF NOT EXISTS t_setting (
    `key` VARCHAR(100) NOT NULL PRIMARY KEY COMMENT '设置键名',
    value VARCHAR(255) COMMENT '设置值'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_user (id, id_card, name, gender, birth_date, phone_number, address, emergency_contact_name, emergency_contact_phone,
                    is_single_alone, is_couple_alone, is_mobility_impaired, is_reliance_equipment, is_chronic_disease, is_reliance_environment, is_production,
                    power_supply_area, community, meter_number, community_manager_name, community_manager_phone, user_type)
VALUES (1, '110101195001010001', '张秀玲', '女', '1950-01-01', '13800000001', '山西省阳泉市城区上站街道市政府社区桃南中路一巷7号楼1单元101', '张师傅', '15110860696',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '西河滩中变台', '政府社区', '232304540251', '冯', '19935335081', '住宅用户'),
        (2, '110101195001010002', '荆素红', '女', '1950-01-01', '13800000002', '阳泉市城区义井街道义井社区居委会义井西街燕竹花园小区6号楼4单元6号', '荆师傅', '15364531366',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '燕竹花园1#箱变', '鸿龙社区', '202202119233', '胡', '19935335408', '住宅用户'),
        (3, '110101195001010003', '胡庭栋', '男', '1950-01-01', '13800000003', '阳泉市城区义井街道义井社区居委会义井西街燕竹花园小区16号楼2单元401', '胡师傅', '13393532296', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '燕竹花园3#箱变', '鸿龙社区', '192109480288', '胡', '19935335408', '住宅用户'),
        (4, '110101195001010004', '张玉楠', '女', '1950-01-01', '13800000004', '山西省阳泉市城区上站街道窑坡社区南大街路河坡小区3号楼1单元401', '张师傅', '18735321798',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '河坡电厂箱变', '窑坡社区', '202201142896', '张', '19935335096', '住宅用户'),
        (5, '110101195001010005', '郭晓琼', '男', '1950-01-01', '13513533522', '后院19号楼1单元7号', '紧急联系人5', '13900000005',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '中心配2', '默认社区', '1430001200021111103366', '社区管理员', '13700000005', '住宅用户'),
        (6, '110101195001010006', '陈艮堂', '女', '1950-01-01', '18235316358', '刘家垴13号楼1单元1号', '紧急联系人6', '13900000006', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '刘家垴3号', '默认社区', '1430001190021094818978', '社区管理员', '13700000006', '住宅用户'),
        (7, '110101195001010007', '王琛', '男', '1950-01-01', '17603531935', '馨安家园4号楼2单元101', '紧急联系人7', '13900000007',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '馨安家园2', '默认社区', '1430001230023044912580', '社区管理员', '13700000007', '住宅用户'),
        (8, '110101195001010008', '钱少英', '女', '1950-01-01', '13703531111', '馨怡家园4号楼19号家', '紧急联系人8', '13900000008', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '馨怡家园1', '默认社区', '1430001230023048606256', '社区管理员', '13700000008', '住宅用户'),
        (9, '110101195001010009', '用户9', '男', '1950-01-01', '18335379838', '中沙坪6-3-5', '马振旺', '18335379838', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '中沙坪1号配电室', '默认社区', '1430001190021081691126', '社区管理员', '13700000009', '住宅用户'),
        (10, '110101195001010010', '用户10', '女', '1950-01-01', '17603538208', '沙坪20-3-19', '赵贵宝', '17603538208', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '中沙坪1号配电室', '默认社区', '1430001230023046211254', '社区管理员', '13700000010', '住宅用户'),
        (11, '110101195001010011', '用户11', '男', '1950-01-01', '13934033725', '沙坪22-2-2', '李玉生', '13934033725', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '沙坪北箱变', '默认社区', '1430001200021109077761', '社区管理员', '13700000011', '住宅用户'),
        (12, '110101195001010012', '用户12', '女', '1950-01-01', '15386734700', '沙坪35-5（-1）', '杜红红', '15386734700',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '沙坪二期2号箱变', '默认社区', '1430001200021103812894', '社区管理员', '13700000012', '住宅用户'),
        (13, '110101195001010013', '用户13', '男', '1950-01-01', '13038071027', '中沙坪5-3-13', '姜林娥', '13038071027', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '中沙坪1号配电室', '默认社区', '1430001190021081689000', '社区管理员', '13700000013', '住宅用户'),
        (14, '110101195001010014', '用户14', '女', '1950-01-01', '15235304542', '里沙坪13-1-2', '赵素莲', '15235304542', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '里沙坪4号箱变', '默认社区', '1430001200021111059595', '社区管理员', '13700000014', '住宅用户'),
        (15, '110101195001010015', '用户15', '男', '1950-01-01', '18435374515', '蒙河25-32', '党宪文', '18435374515', 
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '蒙北1号箱变', '默认社区', '1430001190021081700965', '社区管理员', '13700000015', '住宅用户'),
        (16, '140302193711101611', '张文堂', '男', '1937-11-10', '15635317908', '新华西街北岭一区3号楼2单元102', '紧急联系人16', '15635317188',
        FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE,
        '北岭坡南支2号变台', '北岭坡社区', '202201568100', '小李', '19935335233', '住宅用户');

INSERT INTO t_user_electricity_tags (pattern_id, tag_name)
VALUES (1, '正常用电'),
       (1, '规律用电'),
       (2, '高用电'),
       (2, '医疗设备'),
       (2, '规律用电'),
       (3, '中等用电'),
       (3, '规律用电'),
       (3, '呼吸机用户'),
       (4, '中等用电'),
       (4, '规律用电'),
       (4, '呼吸机用户'),
       (5, '低用电'),
       (5, '规律用电'),
       (5, '其他设备用户'),
       (6, '高用电'),
       (6, '规律用电'),
       (6, '透析机用户'),
       (7, '中等用电'),
       (7, '规律用电'),
       (7, '呼吸机用户'),
       (8, '高用电'),
       (8, '规律用电'),
       (8, '多设备用户'),
       (9, '中等用电'),
       (9, '规律用电'),
       (9, '呼吸机用户'),
       (10, '中等用电'),
       (10, '规律用电'),
       (10, '慢病用户');