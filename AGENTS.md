# AutoCode 项目 AGENTS.md

## 项目定位
历史代码智能分析与开发平台。支持代码图谱索引、Git 历史分析、文档知识库接入、统一检索。

## 技术栈（强制）

- **Java 21** + **Spring Boot 3.3.13**
- **JdbcClient**（禁止使用 JPA / Hibernate）
- **Java `record`** 用于 DTO 和实体
- **PostgreSQL 15+** + **Redis 7**
- **Vue 3 + TypeScript + Element Plus**（前端）
- **Docker Compose**（部署）

## 编码规范（强制）

- 包名：`com.autocode.<模块>.<层>`（例：`com.autocode.project.web.dto`）
- 配置类：`@ConfigurationPropertiesScan` 必须在主类上声明
- 文件编码：**UTF-8 without BOM**
- 禁止使用 Lombok
- 禁止使用 JPA 注解（`@Entity`, `@Table`, `@Repository` via Spring Data JPA）
- 日志格式：`%d{ISO8601} [%thread] %-5level %logger{36} [%X{traceId:-}] - %msg%n`
- Git 分支：新功能使用 `codex/` 前缀

## 项目结构

```text
AutoCode/
├── AGENTS.md              ← 本文件
├── docs/                  ← 文档（设计/计划/治理/进度）
│   ├── specs/             ← 需求 + 架构设计
│   ├── plans/             ← 执行计划 + 任务拆分
│   ├── governance/        ← 开发治理规则
│   ├── quality/           ← 测试/审计/门禁
│   └── context/           ← 当前状态 + 进度记录
├── backend/               ← Maven 多模块后端
│   ├── api-gateway/       ← 统一入口（认证/鉴权/限流/审计）
│   ├── project-service/   ← 项目注册/配置/索引编排
│   ├── context-service/   ← 统一查询编排
│   ├── codegraph-runner/  ← 代码图谱索引
│   ├── history-indexer/   ← Git 历史索引
│   ├── knowledge-indexer/ ← 文档知识索引
│   └── llm-gateway/       ← LLM 模型网关
├── frontend/web-console/  ← Vue 3 前端
├── deploy/                ← Docker Compose 部署
└── integration-tests/     ← 冒烟测试脚本
```

## 开发流程（强制）

1. **读当前状态**：`docs/context/current/01-当前阶段与目标.md` + `02-当前任务状态.md`
2. **读执行计划**：`docs/plans/execution/06-开发任务拆分与执行计划.md`
3. **按任务编号工作**：每个改动必须对应一个任务编号（如 PS-04, CG-02）
4. **门禁检查**：启动前检查任务边界/进入条件/自测边界
5. **实现 + 自测**：代码 + 测试一起提交
6. **文档回写**：更新 `docs/context/current/` + 必要时创建 `docs/context/progress/` 记录

## 文档回写规则（强制）

- 当前状态 → `docs/context/current/02-当前任务状态.md`
- 阶段目标变更 → `docs/context/current/01-当前阶段与目标.md`
- 阶段性推进记录 → `docs/context/progress/YYYY-MM-DD-<任务编号>-进度.md`
- 架构决策 → `docs/context/decisions/YYYY-MM-DD-<主题>-决策.md`
- 禁止在 `progress/` 中维护 "当前状态"、"下一步建议" 等动态信息

## 禁止行为

- 无任务边界直接编码
- 未读当前状态直接启动
- 无验证宣告完成
- 无文档回写结束长任务
- 随意修改无关文档
- 引入 JPA/Hibernate 依赖
- 引入 Lombok 依赖
- 使用非 UTF-8 编码保存文件

## 关键文档速查

| 场景 | 文档 |
|------|------|
| 当前在哪个阶段 | `docs/context/current/01-当前阶段与目标.md` |
| 当前任务是什么 | `docs/context/current/02-当前任务状态.md` |
| 有什么任务要做 | `docs/plans/execution/06-开发任务拆分与执行计划.md` |
| 需求是什么 | `docs/specs/product/01-需求文档-PRD.md` |
| 架构怎么设计的 | `docs/specs/architecture/03-详细设计-LLD.md` |
| 数据库表结构 | `docs/specs/architecture/05-数据库核心表结构与数据字典.md` |
| API 有哪些 | `docs/specs/architecture/07-API端点文档.md` |
| 进度记录在哪 | `docs/context/progress/00-进度索引.md` |
| 如何测试 | `docs/quality/testing/01-测试与验证策略.md` |
| 安全要求 | `docs/quality/audit/02-安全审计与数据脱敏检查清单.md` |