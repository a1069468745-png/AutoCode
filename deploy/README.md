# Docker 部署骨架说明

## 1. 适用范围

本目录用于本地或内网开发环境下启动以下基础设施与占位服务：

- PostgreSQL
- Redis
- Qdrant
- Nginx
- `web-console`
- `api-gateway`
- `project-service`
- `context-service`
- `codegraph-runner`
- `history-indexer`
- `knowledge-indexer`
- `llm-gateway`
- `dev-agent-service`

## 2. 文件说明

- `docker-compose.yml`
  - 基础设施与占位服务编排定义
- `.env`
  - 当前本地开发环境默认配置
- `.env.example`
  - 配置示例模板
- `postgres/init/001_bootstrap.sql`
  - PostgreSQL 首次初始化脚本
- `nginx/`
  - Nginx 启动配置与反向代理模板
- `docker/service-placeholder/`
  - 前后端占位服务共享镜像与运行脚本
- `scripts/manage-services.ps1`
  - 统一服务管理脚本

## 3. Windows 一键命令

### 一键启动全部基础设施与占位服务

```powershell
deploy\start-all.cmd
```

### 停止全部基础设施与占位服务

```powershell
deploy\stop-all.cmd
```

### 启动单个服务

```powershell
deploy\start-service.cmd postgres
deploy\start-service.cmd redis
deploy\start-service.cmd qdrant
deploy\start-service.cmd nginx
deploy\start-service.cmd api-gateway
deploy\start-service.cmd web-console
```

### 停止单个服务

```powershell
deploy\stop-service.cmd postgres
deploy\stop-service.cmd redis
deploy\stop-service.cmd qdrant
deploy\stop-service.cmd nginx
deploy\stop-service.cmd api-gateway
deploy\stop-service.cmd web-console
```

### 重启单个服务

```powershell
deploy\restart-service.cmd postgres
deploy\restart-service.cmd redis
deploy\restart-service.cmd qdrant
deploy\restart-service.cmd nginx
deploy\restart-service.cmd api-gateway
deploy\restart-service.cmd web-console
```

### 执行健康校验

```powershell
deploy\validate.cmd
```

## 4. 默认端口

- PostgreSQL：`5432`
- Redis：`6379`
- Qdrant HTTP：`6333`
- Qdrant gRPC：`6334`
- Nginx：`8080`
- API Gateway：`18080`
- Project Service：`18081`
- Context Service：`18082`
- CodeGraph Runner：`18083`
- History Indexer：`18084`
- Knowledge Indexer：`18085`
- LLM Gateway：`18086`
- Dev Agent Service：`18087`
- Web Console：`18088`

如端口冲突，请优先修改 `.env` 中对应端口。

## 5. 验证结果期望

- PostgreSQL 初始化标记表存在
- Redis `PING` 返回 `PONG`
- Qdrant `/healthz` 可访问
- Nginx `/healthz` 与 `/services` 可访问
- Nginx `/api/healthz` 可转发至 `api-gateway`
- `web-console` 与各后端服务保留正式接入所需端口、健康检查和容器名
