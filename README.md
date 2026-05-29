# AutoCode

## 项目简介

AutoCode 是一个面向内网场景的历史代码智能分析与智能开发平台。当前仓库已经完成一期 MVP 的基础设施、核心数据基线以及多项后端服务工程初始化，具备继续向统一查询与上下文构建能力推进的工程基础。

## 当前仓库结构

- `docs/`：治理规则、需求设计、执行计划、质量门禁与当前上下文文档
- `deploy/`：Docker Compose 部署骨架、Nginx 模板、数据库初始化脚本和管理脚本
- `backend/`：后端多模块目录，包含父工程、BOM、公共模块和服务模块
- `frontend/`：前端工程根目录，当前包含 `web-console` 基础骨架
- `integration-tests/`：跨模块联调与验收入口
- `tools/`：辅助脚本与离线依赖
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
12. `CS-01` Context Service 工程初始化

当前已进入下一阶段优先级切换：`CS-02` 查询意图识别与请求模型。

## 开发顺序

当前建议按以下顺序继续推进：

1. `CS-02` 查询意图识别与请求模型
2. `CS-03` 统一查询适配层
3. 后续按依赖推进 `CS-04`、`CS-05` 以及相关服务协同任务

## 当前提示

- 正式开发前请先读取 `AGENT.md`、`CLAUDE.md` 以及 `docs/indexes/` 下的最小启动文档
- `deploy/` 已是后续所有服务接入的统一部署入口
- `backend/context-service/` 已具备最小健康检查、就绪检查与 Redis 探针回归基线
- 所有实施完成后需要同步更新 `docs/context/` 下的当前状态、进度和必要决策记录
