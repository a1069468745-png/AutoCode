# DB-02 Redis 键规范与缓存策略 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 固化一期 MVP 所需的 Redis 键命名规范、TTL 分层、值结构建议和缓存失效策略，为后续网关、项目服务、上下文服务与索引任务接入提供统一缓存基线。

**Architecture:** 延续当前“先规范、后接入”的推进顺序，本轮不编写任何缓存实现代码，只在文档层完成统一约束。规范围绕项目元数据缓存、热点查询缓存、任务状态缓存和会话上下文缓存四类场景展开，并用统一版本前缀和项目标识确保可追踪与可清理。

**Tech Stack:** `Redis 7`、`Markdown`

---

## 1. 任务边界

- 范围内：
  - 新增 `DB-02` 执行计划文档
  - 新增正式 Redis 规范文档
  - 回写当前阶段、当前任务、索引和进度文档
  - 必要时同步更新 `README.md`
- 范围外：
  - 不编写 Java 缓存接入代码
  - 不定义统一 Cache SDK
  - 不增加新的部署组件或运行脚本
  - 不实现消息广播或分布式失效机制

## 2. 验证方式

- 检查新增文档存在且命名连续
- 检查 `DB-02` 已在当前阶段、当前任务和仓库说明中标记为完成
- 检查 `GW-01` 已成为下一顺序任务
- 检查正式规范文档与设计 spec 的四类缓存范围一致

## 3. 回写目标

- `docs/specs/architecture/06-Redis键规范与缓存策略.md`
- `docs/context/current/01-当前阶段与目标.md`
- `docs/context/current/02-当前任务状态.md`
- `docs/indexes/02-当前阶段索引.md`
- `docs/indexes/03-当前任务索引.md`
- `docs/context/progress/2026-05-28-DB-02-Redis键规范与缓存策略进度.md`
- `README.md`

### Task 1: 固化正式 Redis 规范文档
**Files:**
- Create: `docs/specs/architecture/06-Redis键规范与缓存策略.md`

- [ ] **Step 1: 写明本轮边界和统一命名规则**

```markdown
## 设计范围
- 只定义键名、值结构、TTL 和失效规则
- 不进入服务实现代码

## 键格式
ac:v1:{domain}:p:{projectId}:{entity}:{suffix}
```

- [ ] **Step 2: 逐类写清四种缓存规则**

```markdown
### 项目元数据缓存
- ac:v1:project:p:{projectId}:meta:base

### 热点查询结果缓存
- ac:v1:query:p:{projectId}:code:{queryHash}
```

- [ ] **Step 3: 汇总 TTL、失效策略和风险约束**

```markdown
| 缓存类型 | 推荐 TTL |
| --- | --- |
| 项目基础元数据 | 30 分钟 |
```

### Task 2: 补齐执行计划并形成闭环

**Files:**
- Create: `docs/plans/execution/14-DB-02-Redis键规范与缓存策略计划.md`

- [ ] **Step 1: 记录目标、架构和验证方式**

```markdown
**Goal:** 固化一期 MVP 所需的 Redis 键规范与缓存策略
**Architecture:** 先规范后接入
```

- [ ] **Step 2: 列清回写目标和实施边界**

```markdown
- 范围内：规范、计划、状态回写
- 范围外：不写缓存接入代码
```

### Task 3: 回写当前阶段与任务状态

**Files:**
- Modify: `docs/context/current/01-当前阶段与目标.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/02-当前阶段索引.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Modify: `README.md`
- Create: `docs/context/progress/2026-05-28-DB-02-Redis键规范与缓存策略进度.md`

- [ ] **Step 1: 将 `DB-02` 标记为完成并切换下一任务到 `GW-01`**

```markdown
- 已完成 `DB-02` Redis 键规范与缓存策略
- 下一步进入 `GW-01` API Gateway 工程初始化
```

- [ ] **Step 2: 在阶段文档中补齐完成标志**

```markdown
- PostgreSQL 核心表结构与关键索引已固化
- Redis 键命名、TTL 与失效策略已固化
```

- [ ] **Step 3: 记录本轮产出与验证结论**

```markdown
- 新增正式 Redis 规范文档
- 当前阶段可顺序进入 GW-01
```

### Task 4: 执行文本级一致性验证

**Files:**
- Verify only

- [ ] **Step 1: 检查 `DB-02` 文档链已存在**

```powershell
Get-ChildItem "D:\project\AutoCode\docs\plans\execution" | Select-Object -ExpandProperty Name
```

- [ ] **Step 2: 检查状态文档已切换到 `GW-01`**

```powershell
rg -n "DB-02|GW-01" "D:\project\AutoCode\README.md" "D:\project\AutoCode\docs\context\current" "D:\project\AutoCode\docs\indexes"
```

- [ ] **Step 3: 检查正式规范文档包含四类缓存**

```powershell
rg -n "项目元数据缓存|热点查询结果缓存|任务状态缓存|会话上下文缓存" "D:\project\AutoCode\docs\specs\architecture\06-Redis键规范与缓存策略.md"
```

## 4. 自检

- `DB-02` 边界与设计 spec 一致：是
- 四类缓存全部覆盖：是
- 下一顺序任务切换为 `GW-01`：是
- 未提前引入服务实现代码：是
