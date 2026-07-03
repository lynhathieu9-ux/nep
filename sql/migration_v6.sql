-- =====================================================
-- migration_v6.sql  （功能改造批次：7项问题修复）
-- 执行方式：mysql -uroot -p nep_db < sql/migration_v6.sql
-- =====================================================

-- 问题⑤：知识库附件下载 —— 新增附件URL列
ALTER TABLE nep_knowledge
    ADD COLUMN attachment_url VARCHAR(500) NULL COMMENT '附件文件URL(问题⑤下载用)';

-- 问题⑦：网格员接受任务 —— status 新增 PROCESSING 取值（列已是 VARCHAR，无需改表结构）
--   状态机：PENDING → ASSIGNED →(接受)→ PROCESSING →(提交检测)→ COMPLETED
--   此处仅作说明，无 DDL。

-- 可选：为已有已发布知识库补充演示附件（如已上传文件可手动更新 attachment_url）
-- UPDATE nep_knowledge SET attachment_url = '/images/doc/示例.pdf' WHERE id = 1;
