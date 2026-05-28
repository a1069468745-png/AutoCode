# DB-01 核心表结构与初始化脚本设计

## 1. 背景

当前仓库已经完成 `INF-01` 到 `INF-05`，具备统一目录结构、后端与前端工程骨架、统一配置规则以及正式的 Docker Compose 部署骨架。

进入 `DB-01` 后，首要目标不是实现业务逻辑，而是为后续 `GW-01`、`PS-01`、`CG-03`、`HI-03`、`KI-03`、`LLM-03` 等任务提供稳定、可复用、可迁移的 PostgreSQL 数据基线。

当前 `deploy/postgres/init/` 下仅有 `001_bootstrap.sql`，只负责创建 `app.platform_bootstrap_marker`。核心业务表尚未落地，因此 `DB-01` 是一个干净起点。

## 2. 目标

本轮只完成以下内容：

- 在 `app` schema 下落地 `projects`、`symbols`、`symbol_edges`、`commits`、`commit_symbols`、`requirements`、`documents`、`document_links`、`model_profiles`、`query_logs`
- 为上述表补齐主键、唯一键、外键和关键查询索引
- 让初始化 SQL 能在空库中顺序执行成功
- 产出一份中文数据字典，明确字段用途与约束

本轮不做以下内容：

- 不实现报表类表结构
- 不提前加入统计冗余字段
- 不提前加入复杂 JSON 检查逻辑
- 不进入 Redis 键设计与 Qdrant 集合规划

## 3. 设计原则

### 3.1 字段粒度

采用“最小可用核心字段 + 完整主键/外键/关键索引”的策略。

含义是：

- 只保留 LLD 和后续服务初始化会立即依赖的字段
- 所有表的主键、唯一键、外键、非空要求和核心索引本轮一次固化
- 统计字段、扩展状态字段、复杂审计字段后续按任务边界单独补充

### 3.2 类型约定

- 主键统一使用 `bigserial`
- 跨表引用统一使用 `bigint`
- 时间字段统一使用 `timestamptz`
- 路径、名称、状态、类型字段统一使用 `varchar`
- 不提前引入 PostgreSQL enum，避免后续迁移成本
- 可变结构元数据先使用 `jsonb`

### 3.3 命名与范围

- 所有业务表统一放在 `app` schema
- 表名、字段名保持英文小写下划线风格
- 唯一约束优先按“项目隔离”设计，即大多数业务实体都通过 `project_id` 形成逻辑边界

## 4. 表结构方案

### 4.1 `projects`

用途：项目注册、仓库配置、文档路径与基础状态管理。

核心字段：

- `id`
- `name`
- `repo_url`
- `default_branch`
- `language_stack`
- `doc_repo_path`
- `status`
- `created_at`
- `updated_at`

约束：

- `name` 非空
- `repo_url` 非空
- `name` 唯一

### 4.2 `symbols`

用途：保存 CodeGraph 产出的符号节点。

核心字段：

- `id`
- `project_id`
- `file_path`
- `symbol_name`
- `symbol_kind`
- `signature`
- `line_start`
- `line_end`
- `created_at`

约束：

- `project_id` 外键指向 `projects.id`
- `symbol_name` 非空
- `file_path` 非空
- `line_start <= line_end`

说明：

- 本轮不引入额外哈希字段
- 唯一性不做过强约束，避免不同语言和重载场景误伤

### 4.3 `symbol_edges`

用途：保存符号间调用、引用、继承、实现等关系。

核心字段：

- `id`
- `project_id`
- `source_symbol_id`
- `target_symbol_id`
- `edge_type`
- `created_at`

约束：

- `project_id` 外键指向 `projects.id`
- `source_symbol_id` 外键指向 `symbols.id`
- `target_symbol_id` 外键指向 `symbols.id`
- `edge_type` 非空
- `(project_id, source_symbol_id, target_symbol_id, edge_type)` 唯一

### 4.4 `commits`

用途：保存 Git 提交历史的核心信息。

核心字段：

- `id`
- `project_id`
- `commit_hash`
- `author`
- `commit_time`
- `message`
- `branch_name`
- `created_at`

约束：

- `project_id` 外键指向 `projects.id`
- `commit_hash` 非空
- `(project_id, commit_hash)` 唯一

### 4.5 `commit_symbols`

用途：描述某次提交影响到哪些符号，以及变更类型。

核心字段：

- `id`
- `project_id`
- `commit_id`
- `symbol_id`
- `change_type`
- `created_at`

约束：

- `project_id` 外键指向 `projects.id`
- `commit_id` 外键指向 `commits.id`
- `symbol_id` 外键指向 `symbols.id`
- `(project_id, commit_id, symbol_id, change_type)` 唯一

### 4.6 `requirements`

用途：保存需求条目及其来源文档关联。

核心字段：

- `id`
- `project_id`
- `requirement_code`
- `title`
- `status`
- `source_doc_id`
- `created_at`
- `updated_at`

约束：

- `project_id` 外键指向 `projects.id`
- `source_doc_id` 外键指向 `documents.id`，允许为空
- `requirement_code` 非空
- `title` 非空
- `(project_id, requirement_code)` 唯一

### 4.7 `documents`

用途：保存 Markdown 文档与知识库元数据。

核心字段：

- `id`
- `project_id`
- `doc_path`
- `doc_type`
- `title`
- `metadata_json`
- `created_at`
- `updated_at`

约束：

- `project_id` 外键指向 `projects.id`
- `doc_path` 非空
- `(project_id, doc_path)` 唯一

### 4.8 `document_links`

用途：连接文档、需求、提交和符号，形成追溯关系。

核心字段：

- `id`
- `project_id`
- `document_id`
- `symbol_id`
- `commit_id`
- `requirement_id`
- `created_at`

约束：

- `project_id` 外键指向 `projects.id`
- `document_id` 外键指向 `documents.id`
- `symbol_id`、`commit_id`、`requirement_id` 允许为空
- 至少需要一项关联目标存在

说明：

- 通过 `check` 约束确保 `symbol_id`、`commit_id`、`requirement_id` 至少一个非空
- 本轮不增加 `link_type`，避免提前做复杂分类

### 4.9 `model_profiles`

用途：保存项目级模型策略与网关配置。

核心字段：

- `id`
- `project_id`
- `provider`
- `base_url`
- `model_name`
- `embedding_model`
- `timeout_seconds`
- `fallback_model`
- `enable_local_only`
- `created_at`
- `updated_at`

约束：

- `project_id` 外键指向 `projects.id`
- `provider`、`model_name` 非空
- `timeout_seconds > 0`

说明：

- 本轮不保存真实密钥字段，避免与后续安全策略耦合

### 4.10 `query_logs`

用途：保存查询与模型调用的审计基础数据。

核心字段：

- `id`
- `project_id`
- `user_id`
- `query_type`
- `query_text`
- `model_used`
- `cost_token`
- `created_at`

约束：

- `project_id` 外键指向 `projects.id`
- `query_type` 非空
- `query_text` 非空
- `cost_token >= 0`

说明：

- `user_id` 本轮用 `varchar` 兼容未来认证接入前后的不同身份来源

## 5. 索引策略

本轮只建设关键索引，不做过度优化。

计划包含：

- `symbols(project_id, symbol_name)`
- `symbols(project_id, file_path)`
- `symbol_edges(project_id, source_symbol_id)`
- `symbol_edges(project_id, target_symbol_id)`
- `commits(project_id, commit_time desc)`
- `commit_symbols(project_id, symbol_id)`
- `requirements(project_id, requirement_code)`
- `documents(project_id, doc_path)`
- `query_logs(project_id, created_at desc)`

## 6. 初始化脚本拆分

沿用当前初始化目录，调整为三段：

1. `001_bootstrap.sql`
   - 保留 `app` schema 与平台标记表
2. `002_schema.sql`
   - 创建全部核心业务表与约束
3. `003_index.sql`
   - 创建关键索引

这样做的原因是：

- schema 与数据标记职责清晰
- 表结构与索引分离，后续更容易回看和扩展
- 便于在验证时单独定位“建表失败”还是“索引失败”

## 7. 错误处理与兼容性

- 所有 `create table`、`create index` 使用可重复执行策略，避免开发环境重复初始化失败
- 外键默认使用 `on delete cascade` 只作用于明确从属关系的数据，例如项目删除带走相关从表
- 对于 `documents -> requirements` 这类存在初始化顺序敏感的关系，按先建主表再加外键的方式处理

## 8. 验证方案

本轮验证分三层：

1. 静态验证
   - 检查 SQL 文件存在且顺序清晰
2. 空库执行验证
   - 使用 PostgreSQL 容器在空库执行初始化脚本
3. 结构验证
   - 查询 `information_schema.tables`、`pg_indexes`，确认目标表和关键索引存在

## 9. 风险与取舍

### 9.1 本轮主动不做的事

- 不提前设计复杂统计字段
- 不提前设计多租户组织级表
- 不提前引入向量数据表
- 不把认证用户体系直接耦合进当前 schema

### 9.2 这样做的好处

- 保证 `DB-01` 边界稳定
- 降低后续服务初始化时的返工概率
- 让后续 `DB-02`、`GW-01`、`PS-01` 能明确接在哪些表上

## 10. 本轮实施输出

实施完成后应至少产出：

- `deploy/postgres/init/002_schema.sql`
- `deploy/postgres/init/003_index.sql`
- 一份中文数据字典文档
- 当前阶段与任务进度回写
