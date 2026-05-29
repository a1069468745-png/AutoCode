# AutoCode

历史代码智能分析与开发平台。

## 硬约束（始终生效）

- Java 21 + Spring Boot 3.3.13，JdbcClient（禁止 JPA/Hibernate），Java record
- 包名 `com.autocode.<模块>.<层>`，UTF-8 without BOM
- 禁止 Lombok，禁止 JPA 注解
- Git 分支前缀 `codex/`

## 文档按需加载

**不要全量读取 docs/。** 先读 `docs/AGENTS.md` 获取加载策略，按当前任务类型按需加载。

## 提交前必须

- 代码 + 测试一起验证通过
- 更新 `docs/context/current/02-当前任务状态.md`
- 阶段性推进时追加 `docs/context/progress/` 记录