# docs/ 目录 AGENTS.md

## 文档导航规则（agent 必读）

### 首次进入项目时，按顺序读取：

1. `context/current/01-当前阶段与目标.md` — 知道当前阶段
2. `context/current/02-当前任务状态.md` — 知道当前任务
3. `plans/execution/06-开发任务拆分与执行计划.md` — 知道任务体系

### 编码实现前，补充读取：

4. `specs/product/01-需求文档-PRD.md` — 需求边界
5. `specs/architecture/03-详细设计-LLD.md` — 架构约束
6. `governance/01-开发治理规则.md` — 工作方式约束

### 提交前，检查：

7. `governance/02-门禁与审计规则.md` — 门禁条件
8. `quality/testing/01-测试与验证策略.md` — 验证要求

## 目录约定

| 目录 | 性质 | 何时写入 | agent 何时读取 |
|------|------|---------|---------------|
| `context/current/` | 当前事实 | 每次任务完成时更新 | 每次会话启动时 |
| `context/progress/` | 历史记录 | 阶段性推进时追加 | 需要追溯脉络时 |
| `context/decisions/` | 持久决策 | 架构/规则变更时 | 遇到冲突时参考 |
| `specs/product/` | 需求权威 | 产品范围变更时 | 编码前确认边界 |
| `specs/architecture/` | 设计权威 | 架构变更时 | 编码前确认方案 |
| `plans/execution/` | 执行权威 | 任务拆分变更时 | 选择任务时 |
| `plans/tasks/` | 任务入口 | 新模块加入时 | 进入新模块时 |
| `governance/` | 工作规则 | 规则变更时 | 每次编码前 |
| `quality/testing/` | 测试规范 | 策略变更时 | 编码后验证前 |
| `quality/audit/` | 审计规则 | 安全要求变更时 | 涉及安全时 |
| `indexes/` | 导航辅助 | 文档结构变更时 | 需要发现文档时 |

## 禁止行为

- 不要把所有 `progress/` 下的历史记录全量读取
- 不要在 `progress/` 中维护 "当前状态" 等动态信息
- 不要修改 `superpowers/` 目录下的文件（临时产物）
- 不要把短期状态写入 `decisions/` 或 `governance/`