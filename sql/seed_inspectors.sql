-- =====================================================
-- seed_inspectors.sql  新增网格员演示数据（便于管理员按姓名指派有充足选项）
-- 密码统一 123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
-- 执行：mysql -uroot -p --default-character-set=utf8 nep_db < sql/seed_inspectors.sql
-- =====================================================

-- 幂等：先按手机号清理旧的演示网格员（连带其网格分配）
DELETE g FROM nep_grid_assignment g
  JOIN nep_user u ON g.inspector_id = u.id
  WHERE u.phone IN ('18800000010','18800000011','18800000012','18800000013','18800000014',
                    '18800000015','18800000016','18800000017','18800000018','18800000019');
DELETE FROM nep_user WHERE phone IN ('18800000010','18800000011','18800000012','18800000013','18800000014',
                                     '18800000015','18800000016','18800000017','18800000018','18800000019');

-- 新增网格员（北京多名→支持本地指派；其余省份→支持就近异地指派）
INSERT INTO nep_user (phone, password, real_name, age, gender, role, employee_code, province_id, city_id, status) VALUES
('18800000010','e10adc3949ba59abbe56e057f20f883e','王建国',35,1,'NEPG','NEPG002',1,1,1),
('18800000011','e10adc3949ba59abbe56e057f20f883e','李明',29,1,'NEPG','NEPG003',1,1,1),
('18800000012','e10adc3949ba59abbe56e057f20f883e','赵强',41,1,'NEPG','NEPG004',1,15,1),
('18800000013','e10adc3949ba59abbe56e057f20f883e','孙丽',33,0,'NEPG','NEPG005',2,30,1),
('18800000014','e10adc3949ba59abbe56e057f20f883e','周涛',38,1,'NEPG','NEPG006',4,50,1),
('18800000015','e10adc3949ba59abbe56e057f20f883e','吴敏',27,0,'NEPG','NEPG007',6,70,1),
('18800000016','e10adc3949ba59abbe56e057f20f883e','郑华',45,1,'NEPG','NEPG008',7,90,1),
('18800000017','e10adc3949ba59abbe56e057f20f883e','冯军',31,1,'NEPG','NEPG009',9,110,1),
('18800000018','e10adc3949ba59abbe56e057f20f883e','陈静',36,0,'NEPG','NEPG010',10,130,1),
('18800000019','e10adc3949ba59abbe56e057f20f883e','杨波',40,1,'NEPG','NEPG011',12,150,1);

-- 同步写入网格区域分配（status=1 可工作），供智能推荐"本地/就近"匹配
INSERT INTO nep_grid_assignment (inspector_id, province_id, city_id, status)
SELECT id, province_id, city_id, 1 FROM nep_user
WHERE phone IN ('18800000010','18800000011','18800000012','18800000013','18800000014',
                '18800000015','18800000016','18800000017','18800000018','18800000019');

-- 确保原有网格员 张网格(id 7) 也有网格分配（北京东城区）
INSERT INTO nep_grid_assignment (inspector_id, province_id, city_id, status)
SELECT 7, 1, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM nep_grid_assignment WHERE inspector_id = 7 AND city_id = 1);
