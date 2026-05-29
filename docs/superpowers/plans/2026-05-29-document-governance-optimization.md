# Document Governance Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不削弱现有治理强度的前提下，压缩仓库文档重复入口，明确规范源与当前事实边界，降低新会话与连续会话的上下文加载成本。

**Architecture:** 本次改造分为四个顺序阶段：先瘦身会话入口，再收敛双入口协议，再拆分门禁与审计职责，最后为长文档补齐统一摘要头。每个阶段只修改当前边界内的文档，并通过全文检索验证重复规则是否收敛、引用是否连通、当前事实是否仍有唯一来源。

**Tech Stack:** Markdown、PowerShell、`rg`

---

## File Structure

- `D:\project\AutoCode\README.md`
  - 仓库对外入口，只保留简介、目录结构、文档入口和开发前建议阅读路径
- `D:\project\AutoCode\AGENT.md`
  - 仓库最高优先级约束、权威来源顺序、禁止事项、语言规范
- `D:\project\AutoCode\CLAUDE.md`
  - Agent 执行协议、任务分流、实施前检查、验证与回写协议
- `D:\project\AutoCode\docs\indexes\00-总索引.md`
  - 新会话最小启动集与按任务跳转入口
- `D:\project\AutoCode\docs\indexes\02-当前阶段索引.md`
  - 当前阶段轻量快照卡
- `D:\project\AutoCode\docs\indexes\03-当前任务索引.md`
  - 当前任务轻量快照卡
- `D:\project\AutoCode\docs\context\current\01-当前阶段与目标.md`
  - 当前阶段事实唯一来源
- `D:\project\AutoCode\docs\context\current\02-当前任务状态.md`
  - 当前任务事实唯一来源
- `D:\project\AutoCode\docs\governance\01-开发治理规则.md`
  - 治理原则定义
- `D:\project\AutoCode\docs\governance\02-门禁与审计规则.md`
  - 唯一门禁规范源
- `D:\project\AutoCode\docs\governance\03-会话与上下文加载规则.md`
  - 唯一加载规范源
- `D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md`
  - 审计留痕规范源
- `D:\project\AutoCode\docs\superpowers\specs\2026-05-29-document-governance-optimization-design.md`
  - 本次实施所依据的设计文档

### Task 1: 入口文档瘦身

**Files:**
- Modify: `D:\project\AutoCode\README.md`
- Modify: `D:\project\AutoCode\docs\indexes\00-总索引.md`
- Modify: `D:\project\AutoCode\docs\indexes\02-当前阶段索引.md`
- Modify: `D:\project\AutoCode\docs\indexes\03-当前任务索引.md`
- Test: `D:\project\AutoCode\README.md`
- Test: `D:\project\AutoCode\docs\indexes\00-总索引.md`
- Test: `D:\project\AutoCode\docs\indexes\02-当前阶段索引.md`
- Test: `D:\project\AutoCode\docs\indexes\03-当前任务索引.md`

- [ ] **Step 1: 为入口文档定义目标内容骨架**

将 `README.md` 收敛为以下结构：

```md
# AutoCode

## 项目简介

## 仓库结构

## 文档入口
- 启动约束：`AGENT.md`
- 执行协议：`CLAUDE.md`
- 最小启动索引：`docs/indexes/00-总索引.md`
- 当前状态：`docs/context/current/`

## 开发前建议读取
1. `AGENT.md`
2. `CLAUDE.md`
3. `docs/indexes/00-总索引.md`
4. `docs/indexes/02-当前阶段索引.md`
5. `docs/indexes/03-当前任务索引.md`
```

将 `docs/indexes/00-总索引.md` 收敛为以下结构：

```md
# 文档总索引

## 最小启动集

## 文档分层职责

## 按任务类型导航

## 当前状态入口
```

将 `docs/indexes/02-当前阶段索引.md` 改为以下快照：

```md
# 当前阶段索引

## 当前阶段
`一期 MVP 数据与服务接入阶段`

## 阶段摘要
当前阶段目标是稳定推进索引与上下文相关服务初始化，并保持既有基础设施与服务骨架可回归验证。

## 下一阶段
`一期 MVP 核心服务实现阶段`

## 关联文档
- `docs/context/current/01-当前阶段与目标.md`
- `docs/plans/execution/06-开发任务拆分与执行计划.md`
```

将 `docs/indexes/03-当前任务索引.md` 改为以下快照：

```md
# 当前任务索引

## 当前主任务
`索引类服务初始化任务推进`

## 当前优先任务
`CS-01 Context Service 工程初始化`

## 状态摘要
当前阶段性目标已切换到上下文服务初始化准备，既有索引服务初始化任务已完成并通过验证。

## 门禁摘要
当前任务继续推进前，应先确认 `CS-01` 对应计划、边界、验证方式和回写目标已明确。

## 建议继续读取
- `docs/context/current/02-当前任务状态.md`
- `docs/plans/tasks/08-Context-Service.md`
- `docs/governance/03-会话与上下文加载规则.md`
```

- [ ] **Step 2: 修改入口文档**

Run:

```powershell
@'
使用 apply_patch 按 Task 1 Step 1 的骨架修改以下文件：
- D:\project\AutoCode\README.md
- D:\project\AutoCode\docs\indexes\00-总索引.md
- D:\project\AutoCode\docs\indexes\02-当前阶段索引.md
- D:\project\AutoCode\docs\indexes\03-当前任务索引.md
'@
```

Expected: 以上四个文件删除重复治理正文，保留入口导航与快照职责。

- [ ] **Step 3: 运行重复入口检索**

Run:

```powershell
rg -n "新会话默认先读|最小启动集|当前建议按以下顺序继续推进|当前进度|开发顺序" README.md docs\indexes
```

Expected: 搜索结果仅保留索引与 README 中必要的入口说明，不再出现展开式重复状态正文。

- [ ] **Step 4: 提交入口瘦身改动**

Run:

```bash
git add README.md docs/indexes/00-总索引.md docs/indexes/02-当前阶段索引.md docs/indexes/03-当前任务索引.md
git commit -m "docs: slim session entry documents"
```

Expected: 生成仅包含入口文档瘦身的独立提交。

### Task 2: 收敛双入口协议

**Files:**
- Modify: `D:\project\AutoCode\AGENT.md`
- Modify: `D:\project\AutoCode\CLAUDE.md`
- Test: `D:\project\AutoCode\AGENT.md`
- Test: `D:\project\AutoCode\CLAUDE.md`

- [ ] **Step 1: 为双入口定义职责边界**

将 `AGENT.md` 调整为只保留以下结构：

```md
# AutoCode Agent 全局约束

## 目的

## 最高优先级规则

## 权威来源顺序

## 硬边界

## 强制回写要求

## 语言规范

## 引用
- 最小启动与按需加载见 `docs/governance/03-会话与上下文加载规则.md`
- 执行协议见 `CLAUDE.md`
```

将 `CLAUDE.md` 调整为只保留以下结构：

```md
# AutoCode Claude 执行协议

## 默认会话启动

## 任务类型分流

## 实施前强制检查

## 实施协议

## 验证协议

## 进度同步协议

## Token 控制规则

## 引用
- 正式门禁定义见 `docs/governance/02-门禁与审计规则.md`
- 正式加载规则见 `docs/governance/03-会话与上下文加载规则.md`
```

- [ ] **Step 2: 修改双入口文件**

Run:

```powershell
@'
使用 apply_patch 修改 D:\project\AutoCode\AGENT.md 和 D:\project\AutoCode\CLAUDE.md，
要求删除相互重复的完整正文，保留单独职责，并补齐到 governance 文档的显式引用。
'@
```

Expected: `AGENT.md` 与 `CLAUDE.md` 仍保留双入口模式，但不再双份承载完整加载和门禁正文。

- [ ] **Step 3: 验证双入口重复是否收敛**

Run:

```powershell
rg -n "默认最小加载集|按任务类型追加加载|设计门禁|开发门禁|阶段进入门禁" AGENT.md CLAUDE.md
```

Expected: `AGENT.md` 与 `CLAUDE.md` 中只保留必要摘要或引用，不再同时保留完整清单式正文。

- [ ] **Step 4: 提交双入口收敛改动**

Run:

```bash
git add AGENT.md CLAUDE.md
git commit -m "docs: clarify agent and claude responsibilities"
```

Expected: 生成仅包含双入口协议收敛的独立提交。

### Task 3: 门禁与审计去重

**Files:**
- Modify: `D:\project\AutoCode\docs\governance\02-门禁与审计规则.md`
- Modify: `D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md`
- Test: `D:\project\AutoCode\docs\governance\02-门禁与审计规则.md`
- Test: `D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md`

- [ ] **Step 1: 定义门禁文档与审计文档的目标结构**

将 `docs/governance/02-门禁与审计规则.md` 调整为门禁清单源：

```md
# 门禁与审计规则

## 文档摘要
- 适用任务：规划、开发、测试、发布
- 何时必须读取：进入实际实施前
- 可跳过内容：与当前任务无关的下游门禁
- 关联权威文档：`AGENT.md`、`CLAUDE.md`
- 更新触发条件：门禁定义变化

## 设计门禁
| 检查项 | 要求 |
| --- | --- |
| 任务边界 | 明确 |
| 上游文档 | 已读取 |

## 开发门禁

## 阶段进入门禁

## 测试门禁

## 发布门禁

## 失败处理
```

将 `docs/quality/audit/01-审计与追溯规则.md` 调整为审计留痕源：

```md
# 审计与追溯规则

## 文档摘要
- 适用任务：所有重要工作
- 何时必须读取：完成实现、准备宣告完成、准备切换任务前
- 可跳过内容：与当前任务无关的低优先级记录建议
- 关联权威文档：`docs/governance/02-门禁与审计规则.md`
- 更新触发条件：回写范围或审计要求变化

## 审计目标

## 最小追溯链

## 必须回写的场景

## 推荐记录位置

## 审计优先级

## 文档更新审计要求
```

- [ ] **Step 2: 修改门禁与审计文件**

Run:

```powershell
@'
使用 apply_patch 修改以下文件：
- D:\project\AutoCode\docs\governance\02-门禁与审计规则.md
- D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md
要求将门禁判断与审计留痕拆开，前者回答“能不能做”，后者回答“做完怎么留痕”。
'@
```

Expected: 门禁文件不再展开审计正文，审计文件不再重复门禁判断清单。

- [ ] **Step 3: 验证主题去重结果**

Run:

```powershell
rg -n "失败处理|设计门禁|开发门禁|最小追溯链|必须回写的场景" docs\governance\02-门禁与审计规则.md docs\quality\audit\01-审计与追溯规则.md
```

Expected: 门禁相关术语主要集中在 `governance/02`，审计相关术语主要集中在 `quality/audit/01`。

- [ ] **Step 4: 提交门禁与审计去重改动**

Run:

```bash
git add docs/governance/02-门禁与审计规则.md docs/quality/audit/01-审计与追溯规则.md
git commit -m "docs: split gatekeeping from audit guidance"
```

Expected: 生成仅包含门禁与审计职责拆分的独立提交。

### Task 4: 补齐摘要头并校验引用链

**Files:**
- Modify: `D:\project\AutoCode\docs\governance\01-开发治理规则.md`
- Modify: `D:\project\AutoCode\docs\governance\02-门禁与审计规则.md`
- Modify: `D:\project\AutoCode\docs\governance\03-会话与上下文加载规则.md`
- Modify: `D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md`
- Modify: `D:\project\AutoCode\docs\superpowers\specs\2026-05-29-document-governance-optimization-design.md`
- Test: `D:\project\AutoCode\docs\governance\01-开发治理规则.md`
- Test: `D:\project\AutoCode\docs\governance\02-门禁与审计规则.md`
- Test: `D:\project\AutoCode\docs\governance\03-会话与上下文加载规则.md`
- Test: `D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md`
- Test: `D:\project\AutoCode\docs\superpowers\specs\2026-05-29-document-governance-optimization-design.md`

- [ ] **Step 1: 为规范类文档补齐统一摘要头**

在每个目标文件正文标题后插入：

```md
## 文档摘要
- 适用任务：
- 何时必须读取：
- 可跳过内容：
- 关联权威文档：
- 更新触发条件：
```

其中 `docs/governance/03-会话与上下文加载规则.md` 应填写如下内容：

```md
## 文档摘要
- 适用任务：新会话启动、连续会话恢复、规划、开发、测试、评审、发布
- 何时必须读取：需要决定本次会话该读哪些文档前
- 可跳过内容：与当前任务类型无关的追加加载条目
- 关联权威文档：`AGENT.md`、`CLAUDE.md`、`docs/indexes/00-总索引.md`
- 更新触发条件：最小启动集、任务类型加载矩阵、连续会话恢复规则变化
```

- [ ] **Step 2: 修改规范类文档并补充摘要内容**

Run:

```powershell
@'
使用 apply_patch 为以下文件补齐文档摘要头，并为每个文件填写最小必要摘要内容：
- D:\project\AutoCode\docs\governance\01-开发治理规则.md
- D:\project\AutoCode\docs\governance\02-门禁与审计规则.md
- D:\project\AutoCode\docs\governance\03-会话与上下文加载规则.md
- D:\project\AutoCode\docs\quality\audit\01-审计与追溯规则.md
- D:\project\AutoCode\docs\superpowers\specs\2026-05-29-document-governance-optimization-design.md
'@
```

Expected: 每个规范类文档都具备可快速判断读取必要性的摘要头。

- [ ] **Step 3: 校验摘要头与引用链**

Run:

```powershell
rg -n "^## 文档摘要|关联权威文档|更新触发条件" docs\governance docs\quality\audit docs\superpowers\specs\2026-05-29-document-governance-optimization-design.md
```

Expected: 所有目标文件都存在统一摘要头，并包含关联权威文档字段。

- [ ] **Step 4: 提交摘要头与引用链改动**

Run:

```bash
git add docs/governance/01-开发治理规则.md docs/governance/02-门禁与审计规则.md docs/governance/03-会话与上下文加载规则.md docs/quality/audit/01-审计与追溯规则.md docs/superpowers/specs/2026-05-29-document-governance-optimization-design.md
git commit -m "docs: add summary headers to governance documents"
```

Expected: 生成仅包含摘要头与引用链完善的独立提交。

### Task 5: 最终回归检查与状态回写

**Files:**
- Modify: `D:\project\AutoCode\docs\context\progress\2026-05-29-文档治理优化实施进度.md`
- Modify: `D:\project\AutoCode\docs\context\current\02-当前任务状态.md`
- Modify: `D:\project\AutoCode\docs\context\decisions\2026-05-29-文档治理瘦身与加载优化决策.md`
- Test: `D:\project\AutoCode\docs\context\progress\2026-05-29-文档治理优化实施进度.md`
- Test: `D:\project\AutoCode\docs\context\current\02-当前任务状态.md`
- Test: `D:\project\AutoCode\docs\context\decisions\2026-05-29-文档治理瘦身与加载优化决策.md`

- [ ] **Step 1: 准备进度、状态、决策回写内容**

`docs/context/progress/2026-05-29-文档治理优化实施进度.md` 使用以下骨架：

```md
# 2026-05-29 文档治理优化实施进度

## 本次目标

## 已完成

## 验证结果

## 后续建议
```

`docs/context/decisions/2026-05-29-文档治理瘦身与加载优化决策.md` 使用以下骨架：

```md
# 2026-05-29 文档治理瘦身与加载优化决策

## 决策主题

## 已采纳决策

## 决策原因

## 影响范围
```

并在 `docs/context/current/02-当前任务状态.md` 中追加一条与本次文档治理优化相关的状态更新，说明该专项已完成或处于何种状态。

- [ ] **Step 2: 修改回写文档**

Run:

```powershell
@'
使用 apply_patch 新增并修改以下文件：
- D:\project\AutoCode\docs\context\progress\2026-05-29-文档治理优化实施进度.md
- D:\project\AutoCode\docs\context\decisions\2026-05-29-文档治理瘦身与加载优化决策.md
- D:\project\AutoCode\docs\context\current\02-当前任务状态.md
'@
```

Expected: 本次治理改造具备进度、状态与决策三类回写记录。

- [ ] **Step 3: 运行最终回归检查**

Run:

```powershell
rg -n "最小启动集|先计划|门禁|审计|当前任务索引|当前阶段索引" README.md AGENT.md CLAUDE.md docs\indexes docs\governance docs\quality docs\context\current
```

Expected: 规则仍然存在，但分布符合“规则定义、导航入口、当前事实、审计留痕”分层目标；不存在明显的四处重复整段规则正文。

- [ ] **Step 4: 提交回写与回归检查改动**

Run:

```bash
git add docs/context/progress/2026-05-29-文档治理优化实施进度.md docs/context/current/02-当前任务状态.md docs/context/decisions/2026-05-29-文档治理瘦身与加载优化决策.md
git commit -m "docs: record governance optimization rollout"
```

Expected: 生成包含进度、状态和决策回写的独立提交。
