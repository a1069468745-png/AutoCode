# Redis 键规范与缓存策略

## 1. 文档目标

本文档用于固化一期 MVP 阶段 Redis 的统一使用规则，覆盖以下四类缓存：

- 项目元数据缓存
- 热点查询结果缓存
- 任务状态缓存
- 会话上下文缓存

本文档只定义键命名、值结构建议、TTL 和失效规则，不涉及具体服务实现代码。

## 2. 设计边界

### 2.1 范围内

- 统一键命名格式
- 四类缓存的推荐键结构
- 默认 TTL 分层
- 主动失效和保守清理策略
- 缓存穿透、脏读和键膨胀约束

### 2.2 范围外

- Java 缓存 SDK
- 多实例消息广播失效
- 二期语义缓存与向量缓存
- Redis 持久化和高可用拓扑设计

## 3. 统一命名规范

### 3.1 键格式

统一格式如下：

`ac:v1:{domain}:p:{projectId}:{entity}:{suffix}`

字段说明：

- `ac`：AutoCode 固定前缀
- `v1`：缓存键版本号
- `{domain}`：业务域，例如 `project`、`query`、`task`、`session`
- `p:{projectId}`：项目隔离标识
- `{entity}`：实体或功能类别
- `{suffix}`：主键、哈希、任务 ID、会话 ID 等唯一片段

### 3.2 命名约束

- 所有业务键必须带 `projectId`
- 分隔符统一使用 `:`
- 不在键名中拼接原始长文本
- 查询类键统一使用稳定哈希而不是原始查询串
- 所有缓存键统一保留版本前缀，后续允许通过 `v2` 平滑升级

## 4. 缓存分类规则

### 4.1 项目元数据缓存

用途：

- 缓存项目基础信息
- 缓存项目默认分支和语言栈
- 缓存项目文档目录
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

- 项目基础元数据：30 分钟
- 模型策略摘要：15 分钟

失效规则：

- 项目配置更新后删除 `meta:base`
- 模型策略更新后删除 `meta:model-profile`
- 项目禁用或删除后，清理整个 `project` 域键

### 4.2 热点查询结果缓存

用途：

- 缓存代码调用链查询结果
- 缓存历史追踪查询结果
- 缓存文档命中结果
- 缓存问答上下文聚合结果

推荐键：

- `ac:v1:query:p:{projectId}:code:{queryHash}`
- `ac:v1:query:p:{projectId}:history:{queryHash}`
- `ac:v1:query:p:{projectId}:knowledge:{queryHash}`
- `ac:v1:query:p:{projectId}:ask:{queryHash}`

`queryHash` 建议基于以下内容生成：

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

- 代码查询结果：10 分钟
- 历史查询结果：10 分钟
- 文档查询结果：15 分钟
- 问答上下文结果：5 分钟

失效规则：

- 代码图谱刷新后清理 `query:code:*` 和 `query:ask:*`
- 历史索引刷新后清理 `query:history:*` 和 `query:ask:*`
- 文档索引刷新后清理 `query:knowledge:*` 和 `query:ask:*`

约束说明：

- 优先缓存结构化查询结果，不缓存最终自然语言答案
- `ask` 结果依赖多路数据源，TTL 最短

### 4.3 任务状态缓存

用途：

- 缓存索引任务和分析任务的状态
- 为前端轮询和网关查询提供低延迟读取入口

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

- 运行中任务：24 小时
- 已完成任务：6 小时
- 已失败任务：24 小时
- 最新任务快捷键：24 小时

失效规则：

- 任务状态变化时直接覆盖同键
- 任务结束后按最终状态重设 TTL
- 项目删除后清理整个 `task` 域键

约束说明：

- `latest:{taskType}` 只用于快速展示最近任务
- 真正的任务状态主键仍然以 `job:{taskId}` 为准

### 4.4 会话上下文缓存

用途：

- 缓存最近一次分析请求的结构化上下文
- 缓存连续追问过程中的中间结果
- 减少短时间重复聚合的计算开销

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

- 会话主上下文：30 分钟
- 会话步骤结果：15 分钟

失效规则：

- 每次访问可刷新 TTL
- 会话主动结束后删除对应 session 前缀键
- 超时后自动淘汰，不额外补偿

约束说明：

- 允许采用滑动过期
- 不缓存超大原始文本，避免键值体积不可控

## 5. TTL 总表

| 缓存类型 | 推荐 TTL |
| --- | --- |
| 项目基础元数据 | 30 分钟 |
| 模型策略摘要 | 15 分钟 |
| 代码查询结果 | 10 分钟 |
| 历史查询结果 | 10 分钟 |
| 文档查询结果 | 15 分钟 |
| 问答上下文结果 | 5 分钟 |
| 运行中任务状态 | 24 小时 |
| 已完成任务状态 | 6 小时 |
| 已失败任务状态 | 24 小时 |
| 会话主上下文 | 30 分钟 |
| 会话步骤结果 | 15 分钟 |

## 6. 统一失效策略

### 6.1 通用规则

- 优先采用“数据库写成功后删缓存”
- 当影响范围难以精确判定时，允许按项目域前缀做保守清理
- 对于索引刷新类事件，宁可多删，也不保留高风险脏缓存

### 6.2 推荐清理范围

- 项目配置变更：`ac:v1:project:p:{projectId}:*`
- 代码图谱刷新：`ac:v1:query:p:{projectId}:code:*` 和 `ac:v1:query:p:{projectId}:ask:*`
- 历史索引刷新：`ac:v1:query:p:{projectId}:history:*` 和 `ac:v1:query:p:{projectId}:ask:*`
- 文档索引刷新：`ac:v1:query:p:{projectId}:knowledge:*` 和 `ac:v1:query:p:{projectId}:ask:*`
- 项目删除或禁用：`ac:v1:*:p:{projectId}:*`

## 7. 风险与约束

### 7.1 缓存穿透

对于不存在项目或明显非法请求，不建议写入长 TTL 空结果。如后续有需要，可对空结果使用 30 到 60 秒短 TTL 防穿透键。

### 7.2 脏读风险

查询类缓存最容易在索引刷新后过时，因此建议值结构中保留 `sourceRevision` 字段，为后续服务接入时做版本比对预留能力。

### 7.3 键膨胀

高风险区域主要是 `query` 和 `session` 域，控制规则如下：

- 查询键必须使用归一化参数哈希
- 会话键必须带 TTL
- 不缓存超大原始文本结果
- 不允许一次请求拆出无上限的步骤键

## 8. 接入建议

后续 `GW-*`、`PS-*`、`CS-*` 和各索引服务任务在接入 Redis 时，应遵循以下顺序：

1. 先按本文档定义键格式和 TTL
2. 再在具体服务中落地读写逻辑
3. 最后按实际事件补充失效触发点

在进入服务实现前，不额外扩展新的缓存类型，避免提前分叉规范。
