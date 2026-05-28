# Knowledge Indexer 模块任务入口

## 1. 对应总计划

- `KI-01` 到 `KI-04`

## 2. 模块目标

将 Obsidian/Git Markdown 中的需求、设计、复盘文档转成可查询的结构化知识。

## 3. 任务边界

- 范围内：Markdown 扫描、frontmatter 校验、文档入库、文档关联构建
- 范围外：不做 embedding 和自动修正文档

## 4. 进入门禁

- 已明确 frontmatter 模板
- 已明确文档与代码、提交、需求的关联规则

## 5. 关键验证

- 文档扫描幂等
- 模板问题可告警
- 文档与需求、代码可回溯

## 6. 关键回写

- `docs/context/progress/`
- 文档模板变化时更新治理或设计文档
