# 2026-05-28 KI-01 Knowledge Indexer 最小初始化设计

## 1. 背景

当前仓库已经按顺序完成 `GW-01`、`PS-01`、`CG-01` 和 `HI-01`，当前任务顺位切换到 `KI-01`。  
`backend/knowledge-indexer` 目前仅包含 `pom.xml` 与 `application.yml` 占位，尚未具备可独立启动的 Spring Boot 入口，也没有用于受控管理文档根目录、识别 Markdown 与 YAML frontmatter 的最小组件基线。

结合模块任务入口与当前阶段顺序要求，本轮只完成 `KI-01` 的工程初始化，不提前进入真实文档扫描、数据库入库、frontmatter 模板校验告警或文档关联构建。

## 2. 目标

本轮目标是让 `knowledge-indexer` 从空骨架演进为可启动、可配置、可验证的最小 Spring Boot 服务骨架，为后续 `KI-02` 到 `KI-04` 的真实文档索引能力预留稳定入口。

具体包括：

- 补齐 Spring Boot 启动入口
- 引入 `autocode.knowledge` 配置基线
- 落地文档根目录受控准备服务
- 落地最小 Markdown/frontmatter 识别组件
- 补齐应用上下文加载测试、路径校验测试和解析测试

## 3. 非目标

本轮明确不做以下内容：

- 不执行真实文档目录递归扫描
- 不实现 `documents`、`requirements` 等表的入库逻辑
- 不实现 frontmatter 模板字段校验与告警
- 不实现文档与代码、提交、需求的关联构建
- 不引入 embedding、向量索引或对外 API

## 4. 方案比较

### 方案 A：只补启动类

- 内容：只增加 Spring Boot 启动类和基础上下文测试
- 优点：工作量最小
- 缺点：无法把 `KI-01` 计划中强调的“Markdown/frontmatter 识别基线”沉淀为可测试代码，后续仍需回头补初始化边界

### 方案 B：最小初始化基线

- 内容：补启动类、配置属性、文档根目录受控服务、Markdown/frontmatter 识别组件和基础测试
- 优点：既控制范围，又把后续必需的“文档根目录”和“解析基线”提前立住
- 缺点：比纯启动类多一层解析实现和测试

### 方案 C：初始化加最小文档扫描入口

- 内容：在方案 B 基础上，再增加目录遍历和单批次扫描入口
- 优点：更接近后续真实能力
- 缺点：会提前触碰 `KI-02` 的扫描幂等、文件过滤和入库边界，容易越过 `KI-01`

## 5. 选型

本轮采用方案 B。

原因：

- 它与 `CG-01`、`HI-01` 的完成深度一致，适合作为索引类服务初始化的统一节奏
- 它覆盖了 `KI-01` 真正要求的服务骨架和解析基线
- 它不会提前把 `KI-02+` 的扫描、校验和关联逻辑带进来

## 6. 设计

### 6.1 模块结构

本轮新增以下最小结构：

- `com.autocode.knowledge.KnowledgeIndexerApplication`
- `com.autocode.knowledge.config.KnowledgeIndexerProperties`
- `com.autocode.knowledge.workspace.KnowledgeWorkspaceService`
- `com.autocode.knowledge.parse.MarkdownDocumentParser`
- `com.autocode.knowledge.parse.ParsedMarkdownDocument`

其中：

- `KnowledgeIndexerApplication` 负责模块启动和配置属性扫描
- `KnowledgeIndexerProperties` 负责收敛文档根目录、默认文档扩展名和 frontmatter 分隔符等初始化参数
- `KnowledgeWorkspaceService` 负责基于配置解析并校验文档根目录，防止越界和空路径
- `MarkdownDocumentParser` 负责识别 Markdown 文本中的 YAML frontmatter 与正文
- `ParsedMarkdownDocument` 作为最小解析结果对象，仅承载 frontmatter 映射、正文和 frontmatter 是否存在

### 6.2 配置设计

当前 `application.yml` 中通用的数据源和 Redis 配置保持不变，模块自有配置从现有的 `autocode.docs-root` 收敛为 `autocode.knowledge.*` 命名空间，建议包含：

- `docs-root`
- `markdown-extensions`
- `frontmatter-delimiter`

这样后续 `KI-02` 接入真实扫描时，不需要再调整配置命名。

### 6.3 文档根目录规则

本轮只固化最小、稳定、可测试的规则：

- `docsRoot` 不能为空
- `docsRoot` 必须可归一化为绝对路径
- 最终根目录必须落在配置声明的受控根路径之内
- 解析单个文档路径时，不允许通过 `..` 逃逸到文档根目录之外

这里的目标不是建立完整扫描器，而是先把“文档访问必须经过受控根目录约束”沉淀为代码边界。

### 6.4 Markdown/frontmatter 识别规则

本轮只实现最小识别能力，不做语义校验：

- 如果文档以 frontmatter 分隔符开头，则尝试读取首段 YAML frontmatter
- frontmatter 结束后剩余内容视为 Markdown 正文
- 如果文档不以分隔符开头，则视为“无 frontmatter 的普通 Markdown”
- frontmatter 解析结果使用 `Map<String, Object>` 承载，保持后续扩展空间
- 若 frontmatter 格式非法，则抛出明确异常，先确保错误可见而不是静默吞掉

### 6.5 测试设计

按最小 TDD 闭环补三类测试：

- 应用上下文测试：验证 Spring Boot 上下文可启动，并能正确注入 `KnowledgeIndexerProperties`
- 工作区测试：验证文档根目录归一化、正常文件解析和越界路径拒绝
- 解析测试：验证“带 frontmatter 的 Markdown”“无 frontmatter 的 Markdown”“非法 frontmatter”三类输入行为

## 7. 验证方式

本轮完成判据：

- `mvn -pl knowledge-indexer -am test` 通过
- 测试能覆盖文档根目录越界防护和最小 Markdown/frontmatter 识别
- 不引入超出 `KI-01` 的真实扫描与入库实现代码

## 8. 风险与控制

- 风险：把“初始化”做成“半个扫描器”
  - 控制：禁止加入目录递归遍历、入库和关联逻辑
- 风险：frontmatter 解析能力选得过重，后续维护成本偏高
  - 控制：只引入满足 YAML frontmatter 识别的最小库和最小抽象
- 风险：文档根目录边界过松，后续扫描存在越界访问风险
  - 控制：在初始化阶段先固化绝对路径归一化和根目录约束

## 9. 结论

`KI-01` 应仅交付 Knowledge Indexer 的最小工程初始化骨架：可启动、可配置、可校验、可测试，并具备最小 Markdown/frontmatter 识别基线。  
真实文档扫描、入库、模板校验和文档关联保留到后续 `KI-02` 及以后任务处理。
