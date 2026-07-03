# NEP 7项功能改造/修复 — 交付说明

> 约束：后端为主 + 必要处最小前端接线（用户已批准）；不改 UI 风格与布局。
> 全部 7 项已真实运行验证通过（经网关 http://localhost:9000）。

## 一、修改文件清单

### 后端
| 文件 | 改动 | 问题 |
|---|---|---|
| nep-service-user/.../service/UserService.java | 加 resetPassword 接口声明 | ③ |
| nep-service-user/.../service/impl/UserServiceImpl.java | 实现 resetPassword（MD5） | ③ |
| nep-service-user/.../controller/UserController.java | 新增 GET /inspectors（网格员姓名列表）、PUT /{id}/reset-password | ①③ |
| nep-service-feedback/.../client/UserClient.java | Feign 加 inspectors() | ① |
| nep-service-feedback/.../client/UserClientFallbackFactory.java | inspectors 降级实现 | ① |
| nep-service-feedback/.../entity/SupervisionFeedback.java | 加 STATUS_PROCESSING 常量 | ⑦ |
| nep-service-feedback/.../service/SupervisionFeedbackService.java | 加 accept 声明 | ⑦ |
| nep-service-feedback/.../service/impl/SupervisionFeedbackServiceImpl.java | 实现 accept（ASSIGNED→PROCESSING+归属校验） | ⑦ |
| nep-service-feedback/.../service/impl/AqiDetectionServiceImpl.java | 提交检测放宽允许 PROCESSING 状态 | ⑦ |
| nep-service-feedback/.../controller/FeedbackController.java | 新增 POST /assign-by-name、PUT /accept/{id}、GET /inspector/dashboard | ①⑥⑦ |
| nep-service-content/.../entity/Knowledge.java | 加 attachmentUrl 字段 | ⑤ |
| nep-service-content/.../controller/FileController.java | 新增 POST /image、POST /doc、GET /download | ④⑤ |
| nep-gateway/.../application.yml | content 路由已含 /api/file、/api/decision（早前） | — |
| sql/migration_v6.sql | 新增 attachment_url 列（含 PROCESSING 状态说明） | ⑤⑦ |

### 前端（最小接线，不改风格）
| 文件 | 改动 | 问题 |
|---|---|---|
| nep-ui/src/router/index.js | 补 /admin/knowledge + /admin/knowledge/:id 路由 | ② |
| nep-ui/src/api/user.js | 加 resetUserPassword、getInspectors | ①③ |
| nep-ui/src/api/feedback.js | 加 assignByName、acceptTask | ①⑦ |
| nep-ui/src/api/knowledge.js | 加 downloadKnowledgeFile | ⑤ |
| nep-ui/src/api/file.js（新建） | uploadImage、uploadDoc | ④⑤ |
| nep-ui/src/views/admin/UserManage.vue | 加"重置密码"按钮+handler | ③ |
| nep-ui/src/views/admin/NewsManage.vue | 封面图加上传组件 | ④ |
| nep-ui/src/views/admin/FeedbackManage.vue | 批量派送改姓名下拉+assignByName | ① |
| nep-ui/src/views/KnowledgeList.vue | downloadDoc 真实下载 | ⑤ |
| nep-ui/src/views/KnowledgeDetail.vue | handleDownload 真实下载 | ⑤ |
| nep-ui/src/views/nepg/NEPGHome.vue | 概览数据/任务列表改后端接口 | ⑥ |
| nep-ui/src/views/nepg/NEPGTasks.vue | 加"接受任务"步骤(accept)+PROCESSING状态+tab | ⑦ |

## 二、验证结果（真实运行）
- ① `/api/user/inspectors` → 返回 [ZhangWangGe,李四]；`/assign-by-name` → 按姓名批量派送成功
- ② 管理员点"知识库与规章" → 正常加载 KnowledgeList（不再跳首页）
- ③ `/api/user/7/reset-password` → 密码已重置(code 200)
- ④ `/api/file/image` → 上传返回 /images/news/xxx.png
- ⑤ `/api/file/download?path=...` → HTTP 200 完整回传文件
- ⑥ `/api/feedback/inspector/dashboard` → 真实 {pending,processing,completed,total}
- ⑦ `/api/feedback/accept/{id}` → DB 状态确认变 PROCESSING

## 三、本地验证方法
1. 数据库执行 sql/migration_v6.sql（加 attachment_url 列）
2. 停旧服务→ mvn clean package -DskipTests → 启动 4 服务（或双击 start-all.bat）
3. 前端 npm run dev（3000）
4. 管理员(18800000002/123456)：反馈管理批量勾选→选网格员姓名→派送；账户管理→重置密码；知识库与规章可访问；新闻管理上传封面图
5. 网格员(18800000001/123456)：工作台看真实数据；检测任务→接受任务(状态变检测中)→提交检测
6. 知识库列表/详情→下载（需该知识库已设置 attachmentUrl 附件）

## 四、注意
- 知识库下载需先给记录设置 attachment_url（通过知识库发布/编辑上传附件，或手动 UPDATE）。
- 重启服务前务必先停旧进程（否则 jar 被占用，mvn clean 失败）。
