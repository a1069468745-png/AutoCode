# PS-01 Project Service 工程初始化设计

## 1. 背景

当前仓库已经完成基础设施阶段、数据库基线和网关骨架阶段，下一步按既定顺序进入 `PS-01`。根据 LLD，`project-service` 在一期 MVP 中负责项目注册信息、仓库配置、文档路径和项目级元数据管理，并为后续索引服务、上下文服务和前端控制台提供稳定的项目入口。

当前 `backend/project-service` 仍然只是 Maven 空模块和基础 `application.yml`，还没有形成可独立启动、可落库、可被网关接入的在线服务。因此 `PS-01` 的目标不是一次做完整项目管理，而是先把最小可用的项目服务主链打通：启动、持久化、读取、缓存、统一错误返回和最小自动化测试。

## 2. 本轮目标

本轮只完成以下内容：

- 让 `backend/project-service` 成为可独立启动的 Spring Boot 服务
- 提供 `POST /api/projects`
- 提供 `GET /api/projects`
- 提供 `GET /api/projects/{id}`
- 将项目数据持久化到 `app.projects`
- 为项目详情与项目列表接入最小 Redis 元数据缓存
- 提供统一的参数校验失败、项目不存在、项目名冲突错误结构
- 提供自动化测试，验证最小项目服务闭环

本轮不做以下内容：

- 不实现完整项目 CRUD 中的更新与删除
- 不实现 `/api/projects/{id}/index`
- 不实现 `/api/projects/{id}/model-policy`
- 不写入或读取 `app.model_profiles`
- 不实现成员、角色和权限映射
- 不实现项目状态流转编排，只在创建时写入默认状态
- 不实现批量导入、审批流或复杂搜索筛选

## 3. 设计原则

### 3.1 先打通最小主链，再扩展管理能力

`PS-01` 要优先完成“创建项目、查看列表、查看详情”这条主链。这样可以让前端、网关和后续索引服务尽早拥有正式项目入口，而不是继续依赖文档约定或占位接口。

### 3.2 以既有表结构为准，不反推数据库

数据库基线已经在 `DB-01` 固化，本轮服务实现直接遵循 `app.projects` 的字段、约束和状态范围，不在服务初始化阶段再引入新的表结构或隐式 schema 迁移逻辑。

### 3.3 缓存只做元数据加速，不承载业务真相

Redis 在本轮只承担项目元数据缓存，数据库仍然是唯一权威来源。创建项目后必须主动失效相关列表缓存，详情缓存只在读取路径使用，避免后续状态演化时缓存成为事实来源。

### 3.4 工程初始化保持轻量

这一轮接口简单，数据模型扁平，优先选择轻依赖实现，避免在 `PS-01` 提前引入过重 ORM 或复杂领域分层，把复杂度留给后续真正需要的任务。

## 4. 方案选择

### 方案 A：Spring Web + Spring Data JDBC + RedisTemplate

内容：

- 使用 Spring Boot Web 暴露接口
- 使用 Spring Data JDBC 访问 PostgreSQL
- 使用 RedisTemplate 管理最小缓存

优点：

- 依赖相对轻量
- 保留 Spring 数据访问生态一致性
- 适合简单表结构

缺点：

- 当前项目还没有现成 JDBC 聚合或实体风格约定
- 后续如果主要还是手写 SQL，抽象层收益有限

### 方案 B：Spring Web + JdbcClient 或 NamedParameterJdbcTemplate + RedisTemplate

内容：

- 使用 Spring Boot Web 暴露接口
- 使用 `JdbcClient` 或 `NamedParameterJdbcTemplate` 直接读写 `app.projects`
- 使用 RedisTemplate 管理最小缓存

优点：

- 与现有先定表、后接服务的开发顺序最贴合
- SQL 可读性高，易于严格对齐现有 schema
- 本轮接口简单，代码量可控

缺点：

- 后续复杂查询时需要继续维护手写 SQL
- 需要自己控制行映射和少量样板代码

### 方案 C：Spring Web + Spring Data JPA + RedisTemplate

内容：

- 使用 JPA 实体映射 `app.projects`
- 使用 RedisTemplate 管理最小缓存

优点：

- 上手快
- 基础增删查改开发体验平滑

缺点：

- 对当前已固化 schema 的精细控制不够克制
- 容易在初始化阶段引入额外隐式行为
- 不符合本轮“轻量、可控、直连现有表”的目标

### 结论

本轮选择方案 B。

原因是 `PS-01` 的范围很小，核心在于把 `app.projects` 的创建和读取链路稳定接起来，而不是建立一套完整的数据访问框架。`JdbcClient` 或 `NamedParameterJdbcTemplate` 可以最直接地对齐现有表结构，也能让后续 `PS-02`、`PS-03` 在引入更多字段或查询时保持可控演进。

## 5. 组件设计

### 5.1 `ProjectServiceApplication`

职责：

- 作为 `project-service` 的 Spring Boot 启动入口
- 扫描控制器、服务、仓储和缓存组件
- 提供独立启动能力

输出：

- 服务可通过 `server.port=${PROJECT_SERVICE_PORT:18081}` 启动

### 5.2 `ProjectController`

职责：

- 提供 `POST /api/projects`
- 提供 `GET /api/projects`
- 提供 `GET /api/projects/{id}`
- 接收并校验请求参数
- 将业务结果转为统一 JSON 响应

接口边界：

- 控制器只处理协议层问题，不直接拼接 SQL 或缓存键

### 5.3 `ProjectService`

职责：

- 编排项目创建
- 编排项目列表查询
- 编排项目详情查询
- 处理缓存命中、回填和失效
- 处理业务级异常，例如项目不存在、项目名冲突

行为约束：

- 创建成功后默认状态固定写为 `CREATED`
- 创建成功后必须失效项目列表缓存
- 详情读取优先查缓存，未命中再查库并回填缓存

### 5.4 `ProjectRepository`

职责：

- 直接访问 `app.projects`
- 执行插入、按主键查询、列表查询和按名称冲突判定

建议实现：

- 使用 `JdbcClient`，如当前 Spring Boot 版本不便使用，则退回 `NamedParameterJdbcTemplate`
- SQL 明确写出字段映射，不依赖隐式 ORM 命名规则

读写范围：

- 插入字段：`name`、`repo_url`、`default_branch`、`language_stack`、`doc_repo_path`、`status`
- 查询字段：覆盖 `app.projects` 当前全部业务字段

### 5.5 `ProjectCacheRepository`

职责：

- 封装 Redis 键生成、读取、写入和删除
- 对齐 `DB-02` 的项目元数据缓存规范

建议键位：

- 项目详情：`ac:v1:project-meta:p:{projectId}:project:detail`
- 项目列表：`ac:v1:project-meta:global:project:list`

缓存策略：

- 项目详情 TTL 使用中等时长，建议沿用 `DB-02` 的项目元数据缓存层级
- 项目列表 TTL 可略短于详情
- 创建项目成功后删除项目列表缓存
- 详情查询命中数据库后写入详情缓存

### 5.6 DTO 与响应模型

建议对象：

- `CreateProjectRequest`
- `ProjectSummaryResponse`
- `ProjectDetailResponse`
- `ApiErrorResponse`

设计原则：

- 输入模型只暴露当前允许创建的字段
- 输出模型与数据库记录解耦，避免后续 schema 调整直接泄露到接口层

### 5.7 异常处理组件

职责：

- 统一处理参数校验失败
- 统一处理 `ProjectNotFoundException`
- 统一处理 `ProjectNameConflictException`

建议返回结构：

```json
{
  "code": "PROJECT_NOT_FOUND",
  "message": "Project 12 does not exist",
  "path": "/api/projects/12",
  "timestamp": "2026-05-28T12:00:00Z"
}
```

## 6. 数据与缓存设计

### 6.1 `app.projects` 字段映射

本轮直接使用以下字段：

- `id`
- `name`
- `repo_url`
- `default_branch`
- `language_stack`
- `doc_repo_path`
- `status`
- `created_at`
- `updated_at`

创建规则：

- `name`、`repo_url`、`default_branch` 必填
- `language_stack`、`doc_repo_path` 可选
- `status` 固定初始化为 `CREATED`
- `created_at`、`updated_at` 使用数据库默认值

### 6.2 Redis 缓存范围

本轮只缓存以下两类数据：

- 单个项目详情
- 项目列表结果

本轮不缓存：

- 项目创建请求结果
- 过滤搜索结果
- 模型策略
- 项目状态流转任务

### 6.3 一致性规则

- 数据库写成功之后，才能操作缓存
- 创建项目成功后立即删除项目列表缓存
- 详情缓存采用读后回填，不做预热
- 任一缓存异常都不影响主流程返回，但需要记录日志

## 7. 接口设计

### 7.1 `POST /api/projects`

请求体：

```json
{
  "name": "autocode-platform",
  "repoUrl": "https://git.example.com/team/autocode-platform.git",
  "defaultBranch": "main",
  "languageStack": "Java,TypeScript",
  "docRepoPath": "docs/"
}
```

成功响应建议：

```json
{
  "id": 1,
  "name": "autocode-platform",
  "repoUrl": "https://git.example.com/team/autocode-platform.git",
  "defaultBranch": "main",
  "languageStack": "Java,TypeScript",
  "docRepoPath": "docs/",
  "status": "CREATED"
}
```

失败场景：

- 参数缺失或格式非法返回 `400`
- 项目名重复返回 `409`

### 7.2 `GET /api/projects`

行为：

- 返回项目基础列表
- 默认按 `id desc` 排序

成功响应建议：

```json
[
  {
    "id": 2,
    "name": "autocode-web",
    "repoUrl": "https://git.example.com/team/autocode-web.git",
    "defaultBranch": "main",
    "status": "READY"
  },
  {
    "id": 1,
    "name": "autocode-platform",
    "repoUrl": "https://git.example.com/team/autocode-platform.git",
    "defaultBranch": "main",
    "status": "CREATED"
  }
]
```

缓存规则：

- 允许对整个列表结果做最小缓存
- 创建新项目后必须失效

### 7.3 `GET /api/projects/{id}`

行为：

- 优先查详情缓存
- 未命中时回源 PostgreSQL
- 找不到项目返回 `404`

成功响应建议：

```json
{
  "id": 1,
  "name": "autocode-platform",
  "repoUrl": "https://git.example.com/team/autocode-platform.git",
  "defaultBranch": "main",
  "languageStack": "Java,TypeScript",
  "docRepoPath": "docs/",
  "status": "CREATED",
  "createdAt": "2026-05-28T12:00:00Z",
  "updatedAt": "2026-05-28T12:00:00Z"
}
```

## 8. 依赖与配置设计

### 8.1 Maven 依赖

`backend/project-service/pom.xml` 本轮应至少补齐：

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-jdbc`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-actuator`
- PostgreSQL 驱动
- `spring-boot-starter-test`

### 8.2 配置约束

沿用现有 `application.yml` 中的以下配置：

- `spring.application.name=project-service`
- PostgreSQL 数据源配置
- Redis 连接配置
- `server.port=${PROJECT_SERVICE_PORT:18081}`
- Actuator 暴露 `health,info`

本轮可新增的最小配置：

- Redis 键前缀或 TTL 配置项
- 项目列表缓存 TTL
- 项目详情缓存 TTL

本轮不新增：

- 模型策略相关配置
- 索引触发相关消息队列配置

## 9. 测试与验证设计

### 9.1 测试范围

至少覆盖以下场景：

- 创建项目成功
- 创建项目参数缺失失败
- 创建项目名重复返回冲突
- 查询项目列表成功
- 查询项目详情成功
- 查询不存在项目返回 `404`

### 9.2 测试分层

- 控制器集成测试：基于 `MockMvc` 验证接口、校验和错误结构
- 仓储集成测试：验证 `app.projects` 的插入与查询
- 缓存集成测试：验证详情回填与列表失效的最小行为

### 9.3 验证命令

本轮完成前至少执行：

- `mvn -q -pl project-service -am test`
- `mvn -q -DskipTests package`

如需要数据库与 Redis 环境，优先复用当前 `deploy` 目录已有基础组件。

## 10. 后续衔接

本轮完成后，可以稳定衔接以下任务：

- `PS-02`：补项目更新、删除与更完整状态管理
- `PS-03`：接入项目模型策略，开始使用 `app.model_profiles`
- `PS-04`：补项目索引触发与状态流转
- `GW-*`：将 `/api/projects/**` 纳入正式网关转发
- `CG-*`、`HI-*`、`KI-*`：通过项目基础信息接入首批索引任务

`PS-01` 的完成标准是：项目服务可启动、三个最小接口可用、`app.projects` 读写闭环成立、Redis 元数据缓存最小策略生效、自动化测试和整仓构建验证通过。
