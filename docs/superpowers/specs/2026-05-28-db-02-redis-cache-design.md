# DB-02 Redis 键规范与缓存策略设计
## 1. 背景

当前仓库已经完成 `INF-01` 到 `INF-05`，并完成 `DB-01` PostgreSQL 核心表结构与初始化脚本。进入 `DB-02` 后，本轮目标不是编写缓存接入代码，而是先建立一套后续服务可以统一复用的 Redis 规范。

根据 LLD，Redis 在一期 MVP 中承担以下职责：

- 项目元数据缓存
- 热点查询结果缓存
- 任务状态缓存
- 会话级上下文缓存

这四类缓存都会被后续 `api-gateway`、`project-service`、`context-service`、`codegraph-runner`、`history-indexer`、`knowledge-indexer` 和 `dev-agent-service` 间接或直接依赖，因此需要先统一命名、TTL 和失效规则，避免后续各服务各自定义导致缓存不可追踪、冲突或脏读。

## 2. 本轮目标

本轮只完成以下内容：

- 定义 Redis 键命名规范
- 定义四类缓存的值结构建议
- 定义 TTL 分层和默认时长
- 定义缓存失效和主动清理触发规则
- 定义缓存穿透、脏读、键膨胀的约束策略

本轮不做以下内容：

- 不编写任何 Java 缓存接入代码
- 不定义统一 Cache SDK 或工具类接口
- 不引入消息队列或分布式失效广播机制
- 不提前设计二期语义向量缓存

## 3. 设计原则

### 3.1 项目隔离优先

所有业务缓存键都必须显式带 `project_id`，确保多项目并存时不会互相污染。

### 3.2 读多写少优先

一期 MVP 以查询、索引和分析场景为主，因此缓存设计优先服务于热点读请求，写入侧通过明确失效规则保持一致性，而不追求复杂的实时双写同步。

### 3.3 命名可追踪

键名必须做到“看到键就能判断所属模块、数据类型、作用范围和版本”，便于排查线上问题和执行前缀清理。

### 3.4 TTL 必须显式

除会话锁或短生命周期临时标记外，不允许业务缓存省略 TTL。避免出现永久驻留键造成脏数据和内存不可控增长。

### 3.5 版本前缀预留

所有键统一增加 `ac:v1:` 前缀。后续如键结构升级，可通过 `v2` 平滑切换，而不必一次性全量删库。

## 4. 统一键命名规范

### 4.1 键名格式

统一格式：

`ac:v1:{domain}:p:{projectId}:{entity}:{suffix}`

说明：

- `ac`：平台固定前缀，代表 AutoCode
- `v1`：缓存键版本
- `{domain}`：业务域，如 `project`、`query`、`task`、`session`
- `p:{projectId}`：项目隔离标识
- `{entity}`：实体或功能分类
- `{suffix}`：唯一定位片段，通常为主键、哈希或会话 ID

### 4.2 命名约束

- 分隔符统一使用 `:`
- 不在键名中直接拼接原始长文本
- 查询类键的唯一片段统一使用稳定哈希值
- 会话类键允许使用会话 ID 或线程 ID
- 索引批次或任务类键允许使用任务 ID

## 5. 四类缓存方案

### 5.1 项目元数据缓存

用途：

- 缓存项目基础信息
- 缓存项目默认分支、语言栈、文档路径
- 缓存项目级模型策略摘要

推荐键：

- `ac:v1:project:p:{projectId}:meta:base`
- `ac:v1:project:p:{projectId}:meta:model-profile`

推荐值结构：

```json
{
  "projectId": 12,
  "name": "demo-project",
  "repoUrl": "ssh://git/demo.git",
  "defaultBranch": "main",
  "languageStack": "java,vue",
  "docRepoPath": "/docs",
  "status": "READY",
  "updatedAt": "2026-05-28T10:00:00Z"
}
```

TTL：

- 基础项目元数据：`30m`
- 模型策略摘要：`15m`

失效触发：

- 项目配置更新后立即删除对应项目元数据键
- 模型策略更新后立即删除 `model-profile` 键
- 项目被禁用或删除时按项目前缀清理全部 project 域键

设计取舍：

- 这一类数据来自 `projects` 和 `model_profiles`，更新频率低，适合中等 TTL
- 不做强一致双写，优先采用“数据库写成功后删缓存”

### 5.2 热点查询结果缓存

用途：

- 缓存上下文服务聚合后的热点查询结果
- 缓存高频代码调用链、历史追踪、文档命中、问答上下文结果

推荐键：

- `ac:v1:query:p:{projectId}:code:{queryHash}`
- `ac:v1:query:p:{projectId}:history:{queryHash}`
- `ac:v1:query:p:{projectId}:knowledge:{queryHash}`
- `ac:v1:query:p:{projectId}:ask:{queryHash}`

其中 `queryHash` 建议基于以下内容生成稳定哈希：

- 查询类型
- 归一化后的查询文本
- 过滤条件
- 分支或版本范围
- 请求参数版本号

推荐值结构：

```json
{
  "queryType": "code",
  "queryHash": "9d2b4c4f...",
  "projectId": 12,
  "payload": {},
  "sourceRevision": {
    "projectUpdatedAt": "2026-05-28T10:00:00Z",
    "graphVersion": "cg-20260528-01",
    "historyVersion": "hi-20260528-03",
    "knowledgeVersion": "ki-20260528-02"
  },
  "createdAt": "2026-05-28T10:10:00Z"
}
```

TTL：

- 代码查询结果：`10m`
- 历史查询结果：`10m`
- 文档查询结果：`15m`
- 聚合问答上下文结果：`5m`

失效触发：

- 代码图谱增量或全量重建完成后，清理对应项目 `query:code:*`
- 历史索引增量或全量回放完成后，清理对应项目 `query:history:*`
- 文档索引增量刷新完成后，清理对应项目 `query:knowledge:*`
- 任一底层索引版本变化时，保守清理对应项目 `query:ask:*`

设计取舍：

- 查询缓存比项目元数据更容易过时，因此 TTL 更短
- `ask` 类缓存建立在多来源聚合之上，最容易受索引变动影响，TTL 最短
- 不建议缓存最终大模型生成的自然语言答案，优先缓存结构化上下文结果

### 5.3 任务状态缓存

用途：

- 缓存索引任务、分析任务和异步执行进度
- 为前端轮询和网关查询提供低延迟状态读取

推荐键：

- `ac:v1:task:p:{projectId}:job:{taskId}`
- `ac:v1:task:p:{projectId}:latest:{taskType}`

推荐值结构：

```json
{
  "taskId": "cg-full-20260528-001",
  "taskType": "CODEGRAPH_FULL",
  "projectId": 12,
  "status": "RUNNING",
  "progress": 42,
  "message": "indexing symbols",
  "startedAt": "2026-05-28T10:20:00Z",
  "updatedAt": "2026-05-28T10:25:00Z"
}
```

TTL：

- 运行中任务状态：`24h`
- 已完成任务状态：`6h`
- 已失败任务状态：`24h`
- `latest` 快捷键：`24h`

失效触发：

- 任务状态更新时直接覆盖同键
- 任务结束后按最终状态重设 TTL
- 项目删除时按 `task` 域前缀清理

设计取舍：

- 运行状态需要支持较长任务窗口，因此 TTL 显著长于查询缓存
- 完成态只保留短期可追踪信息，不把 Redis 当永久任务历史库

### 5.4 会话上下文缓存

用途：

- 缓存最近一次或最近若干次分析请求的结构化上下文片段
- 缓存会话级中间结果，减少短时间连续追问的重复聚合开销

推荐键：

- `ac:v1:session:p:{projectId}:ctx:{sessionId}`
- `ac:v1:session:p:{projectId}:ctx:{sessionId}:step:{stepId}`

推荐值结构：

```json
{
  "sessionId": "thread-001",
  "projectId": 12,
  "queryType": "ask",
  "recentFacts": [],
  "recentSymbols": [],
  "recentDocuments": [],
  "lastAccessAt": "2026-05-28T10:30:00Z"
}
```

TTL：

- 会话主上下文：`30m`
- 会话中间步骤结果：`15m`

失效触发：

- 每次访问可刷新 TTL
- 会话主动结束时立即删除对应 session 前缀键
- 超时自动淘汰，不做额外补偿任务

设计取舍：

- 这是最典型的短生命周期缓存，重点是减少连续追问的重复拼装
- 因与用户实时对话强相关，允许采用滑动过期

## 6. TTL 总表

| 缓存类型 | 推荐 TTL |
| --- | --- |
| 项目基础元数据 | 30 分钟 |
| 模型策略摘要 | 15 分钟 |
| 代码/历史查询结果 | 10 分钟 |
| 文档查询结果 | 15 分钟 |
| 问答上下文结果 | 5 分钟 |
| 运行中任务状态 | 24 小时 |
| 已完成任务状态 | 6 小时 |
| 已失败任务状态 | 24 小时 |
| 会话主上下文 | 30 分钟 |
| 会话步骤结果 | 15 分钟 |

## 7. 失效策略

### 7.1 通用规则

- 优先使用“写数据库成功后删缓存”，而不是先更缓存再写数据库
- 不要求精细到单键失效时，允许按项目域前缀做保守清理
- 对于无法准确判断影响范围的索引刷新，宁可多删，不保留高风险脏缓存

### 7.2 建议的前缀清理范围

- 项目配置变更：`ac:v1:project:p:{projectId}:*`
- 代码图谱刷新：`ac:v1:query:p:{projectId}:code:*` 和 `ac:v1:query:p:{projectId}:ask:*`
- 历史索引刷新：`ac:v1:query:p:{projectId}:history:*` 和 `ac:v1:query:p:{projectId}:ask:*`
- 文档索引刷新：`ac:v1:query:p:{projectId}:knowledge:*` 和 `ac:v1:query:p:{projectId}:ask:*`
- 项目删除或禁用：`ac:v1:*:p:{projectId}:*`

### 7.3 不建议本轮引入的能力

- 不引入基于发布订阅的多实例同步失效
- 不引入复杂标签缓存或二级索引键
- 不引入永久缓存回填队列

## 8. 风险与约束

### 8.1 缓存穿透

对不存在项目或明显非法请求，不建议写入长 TTL 空结果。若后续需要，可仅对空结果使用 `30s` 到 `60s` 的短 TTL 防穿透键。

### 8.2 脏读风险

最主要风险来自索引刷新后查询缓存未及时清理，因此所有查询缓存值中建议记录 `sourceRevision`，后续服务接入时可在必要场景做版本比对兜底。

### 8.3 键膨胀

风险最高的是 `query:*` 和 `session:*`。控制手段：

- 查询键必须使用归一化参数哈希
- 会话键必须强制 TTL
- 不缓存超大原始文本结果
- 不允许无上限地为一次请求拆分过多步骤键

### 8.4 热点键覆盖

`latest:{taskType}` 是捷径键，只用于快速展示最近任务，不作为真实任务历史来源。真实任务状态仍应以 `job:{taskId}` 为准。

## 9. 后续实施建议

`DB-02` 实施阶段建议输出以下内容：

- 一份正式 Redis 规范文档
- 一份执行计划文档
- 当前阶段与任务状态回写
- 如有必要，在 `deploy/README.md` 中补充运行期缓存说明

本轮仍不进入服务实现代码，后续由 `GW-*`、`PS-*`、`CS-*` 或索引服务任务在接入时按本规范落地。
