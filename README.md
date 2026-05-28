# AutoCode

## 项目简介

AutoCode 是一个面向内网场景的历史代码智能分析与智能开发平台。当前仓库已完成一期 MVP 的基础设施与工程骨架阶段，具备统一目录结构、后端多模块骨架、前端工程骨架、统一配置规范，以及包含前后端占位服务的 Docker Compose 部署骨架。

## 当前仓库结构

- `docs/`：治理规则、需求设计、执行计划、质量门禁与当前上下文文档
- `deploy/`：Docker Compose 部署骨架、Nginx 反向代理模板、数据库初始化脚本和 Windows 管理脚本
- `backend/`：后端多模块根目录，已包含聚合父工程、BOM、公共模块和 8 个服务模块骨架
- `frontend/`：前端工程根目录，已包含 `web-console` 工程骨架与基础路由、状态管理、请求层入口
- `integration-tests/`：跨模块联调测试、集成验证脚本和验收入口
- `tools/`：辅助工具脚本、离线依赖与开发辅助资源
- `skills/`：仓库内约束技能和执行规范

## 当前进度

已完成：

1. `INF-01` 仓库结构初始化
2. `INF-02` Maven 多模块工程骨架
3. `INF-03` 前端工程骨架
4. `INF-04` 统一配置与环境变量规范
5. `INF-05` Docker Compose 部署骨架扩展
6. `DB-01` 核心表结构与初始化脚本
7. `DB-02` Redis 键规范与缓存策略
8. `GW-01` API Gateway 工程初始化
9. `PS-01` Project Service 工程初始化
10. `CG-01` CodeGraph Runner 工程初始化
11. `HI-01` History Indexer 工程初始化

当前已进入下一阶段：`一期 MVP 数据与服务接入阶段`

## 开发顺序

当前建议按以下顺序继续推进：

1. `KI-01` Knowledge Indexer 工程初始化
2. 逐步补齐 `CS-*`、`LLM-*`

## 当前提示

- 正式开发前请先读取 `AGENT.md`、`CLAUDE.md` 以及 `docs/indexes/` 下的最小启动文档
- `deploy/` 已不再只是基础组件验证目录，而是后续所有服务接入的统一部署入口
- `deploy/postgres/init/` 已具备核心 schema 与关键索引初始化能力
- `docs/specs/architecture/06-Redis键规范与缓存策略.md` 已固化一期缓存键、TTL 与失效规则
- `backend/api-gateway/` 已具备可运行 Spring Boot 网关骨架、鉴权占位与最小测试闭环
- `backend/project-service/` 已具备项目创建、列表、详情三条最小主链，并接入 PostgreSQL 与 Redis 元数据缓存
- 所有实施完成后需要同步更新 `docs/context/` 下的当前状态、进度和必要决策记录
