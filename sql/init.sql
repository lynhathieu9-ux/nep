-- =====================================================
-- 东软环保公众监督系统 (NEP) 数据库初始化脚本
-- =====================================================

-- 设置客户端编码（关键！防止中文乱码）
SET NAMES utf8;

CREATE DATABASE IF NOT EXISTS nep_db
    DEFAULT CHARACTER SET utf8
    DEFAULT COLLATE utf8_general_ci;
USE nep_db;

-- =====================================================
-- 1. 用户表
-- =====================================================
DROP TABLE IF EXISTS nep_user;
CREATE TABLE nep_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password VARCHAR(64) NOT NULL COMMENT '登录密码(MD5加密)',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    age INT COMMENT '年龄',
    gender TINYINT DEFAULT 0 COMMENT '性别(0-女, 1-男)',
    role VARCHAR(10) NOT NULL DEFAULT 'NEPS' COMMENT '角色: NEPS/NEPG/NEPM/NEPV',
    employee_code VARCHAR(50) COMMENT '员工编号',
    province_id BIGINT COMMENT '所属省份ID',
    city_id BIGINT COMMENT '所属城市ID',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用, 1-正常, 2-工作中)',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_phone (phone),
    KEY idx_role (role),
    KEY idx_province (province_id),
    KEY idx_city (city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';

-- =====================================================
-- 2. 省份表
-- =====================================================
DROP TABLE IF EXISTS nep_province;
CREATE TABLE nep_province (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '省份名称',
    code VARCHAR(10) NOT NULL COMMENT '省份代码',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='省份表';

-- =====================================================
-- 3. 城市表 (106个大城市)
-- =====================================================
DROP TABLE IF EXISTS nep_city;
CREATE TABLE nep_city (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '城市名称',
    code VARCHAR(10) NOT NULL COMMENT '城市代码',
    province_id BIGINT NOT NULL COMMENT '所属省份ID',
    city_level VARCHAR(20) COMMENT '城市级别',
    is_capital TINYINT DEFAULT 0 COMMENT '是否省会',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_province (province_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='城市表';

-- =====================================================
-- 4. 公众监督反馈表
-- =====================================================
DROP TABLE IF EXISTS nep_supervision_feedback;
CREATE TABLE nep_supervision_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    supervisor_id BIGINT NOT NULL COMMENT '监督员ID',
    province_id BIGINT NOT NULL COMMENT '省份ID',
    city_id BIGINT NOT NULL COMMENT '城市ID',
    specific_address VARCHAR(500) COMMENT '具体地址',
    estimated_aqi_level INT COMMENT '预估AQI等级(1-6)',
    description VARCHAR(2000) COMMENT '描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/ASSIGNED/COMPLETED',
    assigned_inspector_id BIGINT COMMENT '指派的网格员ID',
    assign_time TIMESTAMP NULL COMMENT '指派时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_supervisor (supervisor_id),
    KEY idx_status (status),
    KEY idx_province (province_id),
    KEY idx_city (city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='公众监督反馈表';

-- =====================================================
-- 5. AQI检测数据表
-- =====================================================
DROP TABLE IF EXISTS nep_aqi_detection;
CREATE TABLE nep_aqi_detection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    feedback_id BIGINT NOT NULL COMMENT '关联的反馈ID',
    inspector_id BIGINT NOT NULL COMMENT '网格员ID',
    province_id BIGINT COMMENT '省份ID',
    city_id BIGINT COMMENT '城市ID',
    so2_aqi INT COMMENT 'SO2 AQI浓度等级',
    co_aqi INT COMMENT 'CO AQI浓度等级',
    pm25_aqi INT COMMENT 'PM2.5 AQI浓度等级',
    final_aqi INT COMMENT '最终AQI等级(MAX(SO2,CO,PM2.5))',
    detection_time TIMESTAMP NULL COMMENT '检测时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_feedback (feedback_id),
    KEY idx_inspector (inspector_id),
    KEY idx_province (province_id),
    KEY idx_city (city_id),
    KEY idx_final_aqi (final_aqi)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='AQI检测数据表';

-- =====================================================
-- 6. 网格员区域分配表
-- =====================================================
DROP TABLE IF EXISTS nep_grid_assignment;
CREATE TABLE nep_grid_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    inspector_id BIGINT NOT NULL COMMENT '网格员ID',
    province_id BIGINT NOT NULL COMMENT '省份ID',
    city_id BIGINT NOT NULL COMMENT '城市ID',
    status TINYINT DEFAULT 1 COMMENT '状态(0-停用, 1-可工作)',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_inspector (inspector_id),
    KEY idx_city (city_id),
    UNIQUE KEY uk_inspector_city (inspector_id, city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='网格员区域分配表';

-- =====================================================
-- 测试数据：省份
-- =====================================================
INSERT INTO nep_province (name, code, sort_order) VALUES
('北京市', '110000', 1),
('天津市', '120000', 2),
('河北省', '130000', 3),
('山西省', '140000', 4),
('辽宁省', '210000', 5),
('吉林省', '220000', 6),
('黑龙江省', '230000', 7),
('上海市', '310000', 8),
('江苏省', '320000', 9),
('浙江省', '330000', 10),
('安徽省', '340000', 11),
('福建省', '350000', 12),
('江西省', '360000', 13),
('山东省', '370000', 14),
('河南省', '410000', 15),
('湖北省', '420000', 16),
('湖南省', '430000', 17),
('广东省', '440000', 18),
('广西壮族自治区', '450000', 19),
('海南省', '460000', 20),
('重庆市', '500000', 21),
('四川省', '510000', 22),
('贵州省', '520000', 23),
('云南省', '530000', 24),
('陕西省', '610000', 25),
('甘肃省', '620000', 26),
('青海省', '630000', 27),
('宁夏回族自治区', '640000', 28),
('新疆维吾尔自治区', '650000', 29);

-- 测试数据：城市（省会）
INSERT INTO nep_city (name, code, province_id, city_level, is_capital, sort_order) VALUES
('北京', '110100', 1, '超大城市', 1, 1),
('天津', '120100', 2, '超大城市', 1, 1),
('石家庄', '130100', 3, 'I型大城市', 1, 1),
('太原', '140100', 4, 'I型大城市', 1, 1),
('沈阳', '210100', 5, '特大城市', 1, 1),
('长春', '220100', 6, 'I型大城市', 1, 1),
('哈尔滨', '230100', 7, '特大城市', 1, 1),
('上海', '310100', 8, '超大城市', 1, 1),
('南京', '320100', 9, '特大城市', 1, 1),
('杭州', '330100', 10, '特大城市', 1, 1),
('合肥', '340100', 11, 'I型大城市', 1, 1),
('福州', '350100', 12, 'I型大城市', 1, 1),
('南昌', '360100', 13, 'I型大城市', 1, 1),
('济南', '370100', 14, '特大城市', 1, 1),
('郑州', '410100', 15, '特大城市', 1, 1),
('武汉', '420100', 16, '超大城市', 1, 1),
('长沙', '430100', 17, '特大城市', 1, 1),
('广州', '440100', 18, '超大城市', 1, 1),
('南宁', '450100', 19, 'I型大城市', 1, 1),
('海口', '460100', 20, 'II型大城市', 1, 1),
('重庆', '500100', 21, '超大城市', 1, 1),
('成都', '510100', 22, '超大城市', 1, 1),
('贵阳', '520100', 23, 'II型大城市', 1, 1),
('昆明', '530100', 24, '特大城市', 1, 1),
('西安', '610100', 25, '特大城市', 1, 1),
('兰州', '620100', 26, 'II型大城市', 1, 1),
('西宁', '630100', 27, 'II型大城市', 1, 1),
('银川', '640100', 28, 'II型大城市', 1, 1),
('乌鲁木齐', '650100', 29, 'II型大城市', 1, 1);
