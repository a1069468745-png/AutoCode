# INF-05 Docker Compose 部署骨架扩展 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有仅含基础组件的 `deploy/` 骨架扩展为包含前端与后端占位服务的正式 Compose 部署入口，为后续 `DB-*` 与服务初始化任务提供稳定承载层。

**Architecture:** 保留 PostgreSQL、Redis、Qdrant 与 Nginx 的基础设施布局，在 Compose 中新增 `api-gateway`、业务后端服务与 `web-console` 的占位容器。占位服务统一复用轻量 HTTP 占位镜像，通过环境变量注入服务名、端口和角色，Nginx 通过模板化反向代理把 `/` 路由到前端占位页、把 `/api/` 路由到 `api-gateway`。

**Tech Stack:** `Docker Compose`、`Nginx`、`Python 3.12-alpine`、`PowerShell`

---

## 1. 任务边界

- 范围内：
  - 扩展 `deploy/docker-compose.yml`
  - 新增前后端占位服务 Dockerfile 与运行脚本
  - 将 `nginx` 反向代理改为模板化配置
  - 扩展 `deploy/.env.example` 与本地 `deploy/.env`
  - 更新部署说明与当前阶段进度文档
- 范围外：
  - 不接入真实 Java 应用镜像
  - 不实现真实前端打包产物发布
  - 不进入数据库建模与业务服务初始化

## 2. 验证方式

- `docker compose config`
- 人工检查 `deploy/docker-compose.yml` 是否包含 `web-console` 与 8 个后端占位服务
- 人工检查 Nginx 模板是否将 `/` 转发至前端、将 `/api/` 转发至网关
- 保留 `mvn -q -DskipTests package` 与 `pnpm.cmd build` 作为阶段回归验证

## 3. 回写目标

- `docs/context/current/01-当前阶段与目标.md`
- `docs/context/current/02-当前任务状态.md`
- `docs/context/progress/2026-05-28-INF-05-Docker-Compose-部署骨架扩展进度.md`
- `README.md`
- `deploy/README.md`

### Task 1: 扩展 Compose 为正式部署骨架

**Files:**
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/.env.example`
- Modify: `deploy/.env`

- [ ] **Step 1: 为占位服务补充统一变量**

```dotenv
SPRING_PROFILES_ACTIVE=local
WEB_CONSOLE_PORT=18088
```

- [ ] **Step 2: 在 Compose 中新增前端与后端占位服务**

```yaml
api-gateway:
  build:
    context: ./docker/service-placeholder
  environment:
    SERVICE_NAME: api-gateway
    SERVICE_PORT: ${API_GATEWAY_PORT}
```

- [ ] **Step 3: 为服务预留健康检查、端口映射与依赖**

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://127.0.0.1:${SERVICE_PORT}/healthz >/dev/null || exit 1"]
```

### Task 2: 新增统一占位镜像

**Files:**
- Create: `deploy/docker/service-placeholder/Dockerfile`
- Create: `deploy/docker/service-placeholder/server.py`

- [ ] **Step 1: 构建统一占位镜像**

```dockerfile
FROM python:3.12-alpine
WORKDIR /app
COPY server.py /app/server.py
CMD ["python", "/app/server.py"]
```

- [ ] **Step 2: 提供健康检查与占位响应**

```python
if self.path == "/healthz":
    self._send_text("ok")
```

- [ ] **Step 3: 前端角色返回 HTML，占位后端返回 JSON**

```python
if SERVICE_ROLE == "frontend":
    self._send_html(...)
else:
    self._send_json(...)
```

### Task 3: 将 Nginx 改为模板化代理

**Files:**
- Modify: `deploy/nginx/nginx.conf`
- Delete: `deploy/nginx/conf.d/default.conf`
- Create: `deploy/nginx/conf.d/default.conf.template`
- Modify: `deploy/README.md`

- [ ] **Step 1: 使用 Nginx 模板目录生成运行时配置**

```yaml
volumes:
  - ./nginx/conf.d:/etc/nginx/templates:ro
```

- [ ] **Step 2: 将 `/` 代理至 `web-console`，`/api/` 代理至 `api-gateway`**

```nginx
location / {
    proxy_pass http://web-console:${WEB_CONSOLE_PORT};
}
```

- [ ] **Step 3: 更新部署说明，说明新增服务占位与验证入口**

### Task 4: 回归验证并切换阶段

**Files:**
- Modify: `README.md`
- Modify: `docs/context/current/01-当前阶段与目标.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Create: `docs/context/progress/2026-05-28-INF-05-Docker-Compose-部署骨架扩展进度.md`

- [ ] **Step 1: 执行 Compose 配置验证**

```powershell
docker compose config
```

- [ ] **Step 2: 执行后端与前端回归构建**

```powershell
mvn -q -DskipTests package
pnpm.cmd build
```

- [ ] **Step 3: 回写阶段状态，准备进入 `DB-01`**

## 4. 自检

- 与 `INF-05` 边界一致：是
- 未提前实现真实业务服务：是
- Compose、Nginx、占位服务与阶段文档形成闭环：是
