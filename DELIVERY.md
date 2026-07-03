# 东软环保公众监督系统（NEP）— 课程设计交付清单

> 严格前后端分离 + SpringCloud 分布式架构｜四角色业务闭环｜交付日期见 git 记录

## 一、验收条件对照（六项全达标）

| # | 验收条件 | 门槛 | 实际 | 结论 |
|---|---|---|---|---|
| 1 | 架构合规：前后端分离 + SpringCloud 分布式落地 | 必须 | Vue3+Axios 独立工程；Gateway+Nacos+Feign+Sentinel 四服务注册运行验证通过 | ✅ |
| 2 | 工作量：总 API≥40 / 单角色≥10 / 页面≥8 / 表≥10 | 量化 | API 122；监督员10/网格员10/管理员28/决策者10；页面23；表18 | ✅ |
| 3 | 角色体系：四角色独立+边界清晰+全链路闭环 | 必须 | @RequiresRole 强制 + 归属校验；端到端闭环运行验证通过 | ✅ |
| 4 | 后端质量：逻辑严谨/异常/校验/无漏洞 | 必须 | 统一异常处理+@Valid DTO+事务+归属校验+堵提权漏洞 | ✅ |
| 5 | 前端质量：美观统一/交互/反馈/权限隔离 | 必须 | Element-Plus 统一 UI+路由守卫+403友好提示+加载/空态 | ✅ |
| 6 | 代码规范：注释/命名/结构/可复用 | 必须 | 分层清晰+全中文注释+复用 Result/BaseEntity/工具类 | ✅ |

## 二、分布式架构（真实运行验证）

- **注册中心 Nacos**：4 服务（gateway/user/feedback/content）全部 `register finished`，实例列表各 1。
- **网关 lb 转发**：路由 `uri: lb://服务名`，经服务发现负载均衡。
- **服务调用 OpenFeign**：feedback/content → user，`FeignRequestInterceptor` 透传 token（验证：管理员智能推荐显示真实网格员姓名）。
- **熔断降级 Sentinel + Fallback**：feedback/content 接入。
- **统一鉴权**：网关 AuthGlobalFilter(JWT) + framework 双拦截器（JwtInterceptor 写 UserContext + RoleInterceptor 校验 @RequiresRole）。

## 三、四角色边界与闭环

| 角色 | 编码 | 专属核心 | 专属接口数 |
|---|---|---|---|
| 监督员 | NEPS | 提交反馈/我的反馈/撤回/评价/趋势/关注区域 | 10 |
| 网格员 | NEPG | 我的任务/接单/提交实测AQI(=MAX)/拒绝/工作量 | 10 |
| 管理员 | NEPM | 智能指派(本地/异地)/转派/批量/升级/归档/用户管理/统计 | 28 |
| 决策者 | NEPV | 8类统计/省分组污染物超标/重点省份预警/综合报告/大屏 | 10 |

**闭环**：监督员提交→管理员智能指派网格员→网格员实测提交(状态→COMPLETED)→决策者大屏统计。已端到端运行验证。

## 四、启动方式（无 Docker，本地原生）

```bash
# 1. 前置：MySQL(3306,root/1234,库nep_db已建) + Nacos standalone(8848)
#    Nacos: nacos/nacos/bin/startup.cmd -m standalone
#    (可选)Redis 6379 / MinIO 9002 —— 未启动时 content 优雅降级
# 2. 数据库：执行 sql/init.sql 及 sql/migration_v2~v5.sql（补齐 escalation_level 等列）
# 3. 构建：mvn clean install -DskipTests
# 4. 启动（顺序）：user(8082) → feedback(8083) → content(8084) → gateway(9000)
#    java -jar nep-service-xxx/target/nep-service-xxx-1.0.0.jar
# 5. 前端：cd nep-ui && npm install && npm run dev
# 6. 验证：http://localhost:8848/nacos 见4服务注册
```

测试账号（密码均 123456）：监督员 13800000001 / 网格员 18800000001 / 管理员 18800000002 / 决策者 18800000003。

## 五、已知事项
- Redis/MinIO 未运行时，热点地图缓存、文件上传等增强功能降级，核心四端业务不受影响。
- MySQL 旧版本不支持 `CREATE INDEX IF NOT EXISTS`（migration_v5 末尾索引语句报错但列已添加成功，不影响功能）。
- transfer/batch-assign/reject 已补齐后端实现。
