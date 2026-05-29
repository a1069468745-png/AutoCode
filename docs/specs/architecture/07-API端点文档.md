# API 端点文档（一期 MVP）

## 1. API Gateway（端口 18080）

| 端点 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/auth/login` | POST | 否 | 本地账号登录，返回 JWT |
| `/api/projects/**` | * | JWT | 代理至 project-service |
| `/api/query/**` | * | JWT | 代理至 context-service（含限流 10rps） |
| `/v1/chat/**` | * | JWT | 代理至 llm-gateway（含限流 5rps） |
| `/api/runner/**` | * | JWT | 代理至各 runner/indexer 服务 |
| `/actuator/health` | GET | 否 | 健康检查 |

## 2. Project Service（端口 18081）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/projects` | POST | 创建项目 |
| `/api/projects` | GET | 列出所有项目 |
| `/api/projects/{id}` | GET | 获取项目详情（含 status/indexError） |
| `/api/projects/{id}/sync-indexes` | POST | 异步触发全量索引（立即返回 INDEXING） |
| `/api/projects/{id}/access` | GET | 查询用户对项目的权限 |
| `/api/projects/{id}/access` | POST | 授予用户项目权限 |

## 3. CodeGraph Runner（端口 18083）

| 端点 | 方法 | 请求体 | 响应 |
|------|------|--------|------|
| `/api/runner/codegraph/index` | POST | `{"projectId":1,"workspaceRoot":"/path"}` | `{"projectId":1,"status":"COMPLETED","symbolCount":50,"edgeCount":120}` |

## 4. History Indexer（端口 18084）

| 端点 | 方法 | 请求体 | 响应 |
|------|------|--------|------|
| `/api/runner/history/index` | POST | `{"projectId":1,"workspaceRoot":"/path","maxCommits":80}` | `{"projectId":1,"status":"COMPLETED","commitCount":20,"linkCount":5}` |

## 5. Knowledge Indexer（端口 18085）

| 端点 | 方法 | 请求体 | 响应 |
|------|------|--------|------|
| `/api/runner/knowledge/index` | POST | `{"projectId":1,"workspaceRoot":"/path","docRepoPath":"docs/"}` | `{"projectId":1,"status":"COMPLETED","documentCount":10,"requirementCount":3,"linkCount":3}` |

## 6. Context Service（端口 18082）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/query/code` | POST | 代码符号查询（按名称/文件/类型） |
| `/api/query/history` | POST | 历史变更查询（按提交/文件/需求） |
| `/api/query/knowledge` | POST | 文档知识查询（按需求/文档/关键词） |
| `/api/query/ask` | POST | 综合问答（五类意图自动路由） |

## 7. LLM Gateway（端口 18086）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/llm/projects/{id}/model-profile` | GET/POST/PUT/DELETE | 项目模型配置 CRUD |
| `/v1/chat/completions` | POST | OpenAI-compatible 占位接口 |

## 认证方式
- 开发模式：Header `Authorization: Bearer dev-token`
- 生产模式：`POST /api/auth/login` 获取 JWT，后续请求携带 `Authorization: Bearer <jwt>`