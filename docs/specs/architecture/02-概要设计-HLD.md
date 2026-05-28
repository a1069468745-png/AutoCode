# 历史代码智能分析与智能开发平台概要设计（HLD）

## 1. 设计目标

本平台面向企业内网研发场景，提供历史代码理解、调用关系分析、Git 历史分析、知识库检索和新需求智能开发辅助能力。系统设计强调以下原则：

- 离线优先：在内网环境可独立运行。
- 多项目复用：支持多人共用和项目快速接入。
- 结构化优先：优先构建代码与历史的结构化事实层。
- 模型解耦：与具体大模型供应商解耦。
- 统一终态：核心技术栈、核心组件、数据模型和接口协议从第一阶段即按终态方案建设。
- 分期启用：平台按能力分阶段上线，不在中途替换核心组件或重构基础技术栈。

## 2. 总体架构

平台总体分为六层：

1. 接入层
- Web Console
- API Gateway

2. 项目治理层
- Project Hub
- 权限与审计模块

3. 数据理解层
- Code Graph Engine
- History Intelligence Engine
- Knowledge Hub

4. 检索编排层
- Context Orchestrator

5. 智能能力层
- LLM Gateway
- Dev Agent Service

6. 存储层
- PostgreSQL
- Redis
- Qdrant
- Git 镜像仓

## 3. 核心模块说明

### 3.1 Project Hub

负责项目注册、仓库配置、分支配置、文档仓绑定、模型策略配置和项目切换，是整个平台的项目管理中枢。

### 3.2 Code Graph Engine

负责将源代码解析为符号、边和文件结构，提供调用关系和影响分析能力。推荐基于 `CodeGraph` 实现。

### 3.3 History Intelligence Engine

负责 Git 历史解析和结构化索引，包括提交、作者、差异、标签、版本和需求编号关联。

### 3.4 Knowledge Hub

负责接入 Obsidian/Git Markdown 文档仓，解析元数据并建立文档与代码、需求、提交之间的关联。

### 3.5 Context Orchestrator

负责统一上下文组织。面对用户查询或新需求输入时，动态决定：

- 需要查哪些索引
- 需要返回哪些结构化事实
- 需要给模型提供哪些上下文

### 3.6 LLM Gateway

负责模型供应商统一接入、路由、熔断、审计和项目级模型策略管理。

### 3.7 Dev Agent Service

负责需求解析、相似需求召回、影响分析汇总、改造建议和测试建议生成，是智能开发闭环的核心执行服务。

## 4. 统一技术栈

平台统一采用如下终态技术栈，并在项目初始建设时一次性定型：

- 服务端：`Java 21 + Spring Boot 3`
- 前端：`Vue 3 + TypeScript + Element Plus`
- 主数据库：`PostgreSQL 15+`
- 缓存：`Redis 7`
- 代码图谱：`CodeGraph`
- Git 历史分析：`JGit + 原生 git 回退`
- 文档知识库：`Obsidian + Git Markdown`
- 全文检索：`PostgreSQL FTS`
- 向量检索：`Qdrant`
- 模型接入：`OpenAI-compatible LLM Gateway`
- 部署：`Docker + Docker Compose`
- 网关代理：`Nginx`

说明：

- 上述组件按终态统一技术方案选型，不以阶段变化为由替换核心底座。
- 阶段之间变化的是能力开放范围，不是底层技术栈和核心组件。

## 5. 关键流程

### 4.1 项目接入流程

1. 在 Project Hub 中注册项目。
2. 拉取代码仓库和文档仓。
3. 执行代码全量索引。
4. 执行 Git 历史全量回放。
5. 执行文档索引和关联构建。
6. 进入增量更新模式。

### 4.2 日常查询流程

1. 用户选择项目。
2. 用户输入自然语言问题或结构化查询。
3. Context Orchestrator 判断查询意图。
4. 调用代码图谱、历史索引、文档索引。
5. 聚合结果并返回给前端或模型。

### 4.3 新需求智能分析流程

1. 用户输入需求。
2. Dev Agent Service 做需求结构化解析。
3. Context Orchestrator 拉取历史相似需求、相关调用链、历史变更和设计文档。
4. LLM Gateway 调用指定模型生成分析结果。
5. 输出改造建议、影响范围和测试建议。

## 6. 部署架构

### 5.1 推荐部署方式

- Web 层部署在内网应用服务器
- PostgreSQL 独立部署
- Git 镜像与索引任务运行在后台服务节点
- 模型网关可与应用同机或独立部署

### 5.2 推荐服务清单

- `web-console`
- `api-gateway`
- `project-service`
- `context-service`
- `history-indexer`
- `codegraph-runner`
- `knowledge-indexer`
- `llm-gateway`
- `postgres`
- `redis`
- `qdrant`

说明：

- 服务清单按平台终态统一建设。
- 不建议采用“第一期缺失核心组件、第二期整体替换”的模式。
- `llm-gateway` 与 `qdrant` 可以在第一阶段完成部署和接口预留，在后续阶段逐步启用相应能力。

## 7. 技术选型建议

### 6.1 代码图谱

- 首选：CodeGraph
- 备选：GitNexus

### 6.2 历史分析存储

- PostgreSQL

### 6.3 文档知识库

- Obsidian + Git Markdown

### 6.4 检索

- 基础检索：PostgreSQL FTS
- 语义检索：Qdrant

### 6.5 模型接入

- OpenAI-compatible Gateway
- 支持 Claude、OpenAI、Gemini、DeepSeek、Qwen、本地模型

## 8. 分期建设原则

### 8.1 第一阶段

- 启用项目接入
- 启用代码图谱索引
- 启用 Git 历史分析
- 启用文档索引
- 启用调用关系查询和历史查询

### 8.2 第二阶段

- 启用语义召回
- 启用多模型统一路由
- 启用混合检索与综合问答

### 8.3 第三阶段

- 启用新需求智能分析
- 启用改造建议生成
- 启用测试建议生成

说明：

- 各阶段均运行在统一终态技术栈之上。
- 后续阶段只增加能力启用范围，不替换核心组件。

## 9. 设计原则

- 不以向量库替代代码关系图谱。
- 不以文档系统替代历史代码结构分析。
- 不让模型直接阅读全仓，必须通过上下文编排裁剪。
- 不在第一阶段引入过重的基础设施，优先保障可落地性。

## 10. 扩展方向

- 接入更多语言与框架解析器
- 引入向量语义召回
- 增强自动化测试影响分析
- 增强需求到代码的自动映射能力
- 增强智能补丁建议与验证能力
