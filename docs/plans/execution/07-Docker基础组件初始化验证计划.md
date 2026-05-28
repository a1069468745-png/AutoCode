# Docker 基础组件初始化验证计划

> **适用场景：** 本计划用于在不越过当前总执行计划依赖链的前提下，完成 `PostgreSQL`、`Redis`、`Qdrant`、`Nginx` 的本地基础初始化验证，并形成 Windows 一键启动与单服务启停脚本。

## 1. 任务定位

- 任务名称：`Docker 基础组件初始化验证`
- 所属阶段：`一期 MVP 实施准备阶段`
- 任务类型：`基础设施预备任务`
- 对应设计依据：
  - `docs/specs/architecture/03-详细设计-LLD.md`
  - `docs/plans/tasks/01-基础设施与工程骨架.md`
  - `docs/plans/tasks/12-测试运维与发布体系.md`

## 2. 任务边界

### 2.1 范围内

- 新增 `deploy/` 目录与基础 Docker 编排文件
- 初始化 `PostgreSQL`、`Redis`、`Qdrant`、`Nginx` 的本地开发配置
- 提供 Windows 一键启动脚本
- 提供单服务启动、停止、重启、状态查看与验证脚本
- 提供最小启动说明和验证说明

### 2.2 范围外

- 不创建 `backend/` 与 `frontend/` 工程骨架
- 不实现 Java 服务和 Vue 前端容器
- 不落地正式业务表结构
- 不启用 `CodeGraph`、`LLM Gateway`、`Dev Agent Service`
- 不扩展到生产级高可用和 Kubernetes

## 3. 进入门禁

开始实施前必须满足：

1. 已完成最小启动集读取。
2. 已确认当前任务属于 `基础设施预备任务`，不是正式 `INF-05` 全量实现。
3. 已存在当前阶段验收边界。
4. 已明确验证方式：
   - `docker compose config`
   - 容器启动结果
   - 单服务停止与重启结果
   - 健康检查结果
5. 已明确本次需要回写的文档：
   - `docs/context/current/`
   - `docs/context/progress/`
   - 必要时更新阶段索引与任务索引

## 4. 实施步骤

1. 创建 `deploy/` 目录与基础文件结构。
2. 编写 `docker-compose.yml`、`.env`、Nginx 配置、PostgreSQL 初始化 SQL。
3. 编写 PowerShell 管理脚本与 `.cmd` 一键入口。
4. 执行 `docker compose config` 校验。
5. 执行一键启动并验证四个基础容器。
6. 验证单服务停止与重启能力。
7. 回写阶段状态、任务状态、进度记录与验收结论。

## 5. 验证边界

### 5.1 必检项

- `docker compose config` 通过
- `postgres`、`redis`、`qdrant`、`nginx` 可启动
- `postgres` 初始化 SQL 生效
- `redis` 可返回 `PONG`
- `qdrant` 健康检查可访问
- `nginx` 健康检查和欢迎页可访问
- 可通过脚本停止单个服务
- 可通过脚本重启单个服务

### 5.2 验证失败处理

- 若配置校验失败，停止启动验证并先修复配置
- 若单服务启停失败，记录失败服务和命令输出
- 若端口冲突，优先通过 `.env` 调整，不直接改脚本逻辑

## 6. 交付物

- `deploy/docker-compose.yml`
- `deploy/.env`
- `deploy/.env.example`
- `deploy/nginx/`
- `deploy/postgres/init/`
- `deploy/scripts/manage-services.ps1`
- `deploy/*.cmd`
- `deploy/README.md`

## 7. 完成门禁

仅当以下条件同时满足时，本任务可标记完成：

- 所有必检项通过
- 当前阶段和任务状态已回写
- 进度记录已回写
- 验证结论已留痕
