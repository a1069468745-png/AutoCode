---
name: progress-sync
description: "在有意义工作完成后同步持久化进度，确保新会话不依赖聊天记录恢复上下文。"
---

# 进度同步

## 目标

在有意义的工作完成后，更新仓库中的持久状态，使未来会话依赖文件而不是记忆恢复上下文。

## 强制更新目标

按任务边界与实际影响范围，更新以下一个或多个位置：

- `/docs/context/progress/`
- `/docs/context/current/02-当前任务状态.md`
- `/docs/context/decisions/`
- plan or architecture docs when scope changed

## 最小记录内容

至少记录：

- 改了什么
- 为什么改
- 当前状态是什么
- 下一步建议是什么

## 更新边界规则

- 必须更新所有应更新文档
- 不得无依据修改无关文档
- 如果发现影响范围超出当前任务边界，先回到计划层对齐再继续
