# 2026-05-28 HI-01 History Indexer 最小初始化设计

## 1. 背景

当前仓库已按顺序完成 `GW-01`、`PS-01` 与 `CG-01`，下一顺位任务进入 `HI-01`。  
`backend/history-indexer` 目前仅有 `pom.xml` 和 `application.yml`，尚未具备可独立启动的 Spring Boot 入口，也没有用于受控管理 Git 历史扫描工作区和扫描范围的最小服务基线。

结合模块任务入口与现阶段顺序要求，本轮只完成 `HI-01` 的工程初始化，不提前进入真实 Git 历史扫描、diff 解析、message 提取或符号级关联落库。

## 2. 目标

本轮目标是让 `history-indexer` 从空骨架演进为可启动、可配置、可验证的最小 Spring Boot 服务骨架，为后续 `HI-02` 到 `HI-04` 的真实历史分析能力预留稳定入口。

具体包括：

- 补齐 Spring Boot 启动入口
- 引入 `autocode.history` 配置基线
- 落地 Git 仓库工作区与扫描范围校验服务
- 补齐应用上下文加载测试和范围校验测试

## 3. 非目标

本轮明确不做以下内容：

- 不执行真实 Git 仓库 clone、fetch 或 checkout
- 不实现提交历史遍历
- 不实现 diff 解析
- 不实现 commit message 需求号提取
- 不实现与 CodeGraph 符号图谱的真实关联
- 不新增数据库表、缓存键或对外 API

## 4. 方案比较

### 方案 A：仅补启动类

- 内容：只增加 Spring Boot 启动类与测试
- 优点：工作量最小
- 缺点：没有把 `HI-01` 模块入口里强调的 Git 来源、历史范围和回退边界沉淀成代码约束，后续还要重做初始化基线

### 方案 B：最小初始化基线

- 内容：补启动类、配置属性、Git 工作区准备与扫描范围校验服务、基础测试
- 优点：既控制范围，又把后续必需的“工作区”和“扫描边界”抽象提前立住
- 缺点：比纯启动类多一点实现量

### 方案 C：初始化加最小 Git 提交枚举

- 内容：在方案 B 基础上，再补最小 JGit 或原生 git 提交枚举入口
- 优点：更接近后续真实功能
- 缺点：会提前触碰历史来源、异常降级和 message 规则，容易越过 `HI-01` 边界

## 5. 选型

本轮采用方案 B。

原因：

- 它与 `CG-01` 的完成度层级一致，适合作为索引类服务初始化的统一节奏
- 它覆盖了 `HI-01` 真正需要的工程初始化骨架
- 它不会提前把后续 `HI-02+` 的历史回放与解析逻辑带进来

## 6. 设计

### 6.1 模块结构

本轮新增以下最小结构：

- `com.autocode.history.HistoryIndexerApplication`
- `com.autocode.history.config.HistoryIndexerProperties`
- `com.autocode.history.workspace.HistoryWorkspaceService`
- `com.autocode.history.scope.HistoryScanScope`

其中：

- `HistoryIndexerApplication` 负责模块启动和配置属性扫描
- `HistoryIndexerProperties` 负责收敛工作区根路径、Git 命令路径、默认扫描分支和默认提交窗口等初始化参数
- `HistoryWorkspaceService` 负责根据 `projectKey` 和 `branchName` 计算并创建受控工作目录
- `HistoryScanScope` 负责表达并校验最小扫描边界，例如分支名、起止提交或最大提交数的约束

### 6.2 配置设计

`application.yml` 中现有的通用数据源和 Redis 配置保持不变，只把模块自有配置从 `autocode.workspace-root` 提升为 `autocode.history.*` 命名空间，建议包含：

- `workspace-root`
- `git-command-path`
- `default-branch`
- `max-commit-window`

这样后续 `HI-02` 接入真实扫描能力时，不需要再改配置命名。

### 6.3 范围校验规则

本轮只固化最小、稳定、可测试的规则：

- `projectKey` 不能为空，且不得包含 `..`
- `branchName` 不能为空，且不得包含 `..`
- 用于落盘目录的分支名要做路径分隔符归一化
- 最终工作目录必须落在配置的 `workspace-root` 之内
- `maxCommitWindow` 必须大于 0
- 如果传入起止提交范围，两端值要么同时为空，要么同时存在

这里的目标不是定义完整历史查询 DSL，而是建立“范围必须先经过结构化校验”的边界。

### 6.4 测试设计

按最小 TDD 闭环补两类测试：

- 应用上下文测试：验证 Spring Boot 上下文可启动，并能正确注入 `HistoryIndexerProperties`
- 范围/工作区测试：验证正常目录创建、路径归一化、越界拒绝和非法提交窗口拒绝

## 7. 验证方式

本轮完成判据：

- `mvn -pl history-indexer -am test` 通过
- 测试能覆盖工作区越界防护和最小范围校验
- 不引入超出 `HI-01` 的扫描实现代码

## 8. 风险与控制

- 风险：把“初始化”做成“半个历史扫描器”
  - 控制：禁止接入真实 Git 命令和提交遍历
- 风险：配置命名后续还要重改
  - 控制：直接采用 `autocode.history.*` 命名空间
- 风险：工作区路径设计不稳定，后续任务重复改
  - 控制：延续 `CG-01` 的受控根目录 + 分支归一化模式

## 9. 结论

`HI-01` 应仅交付 History Indexer 的最小工程初始化骨架：可启动、可配置、可校验、可测试。  
真实 Git 历史扫描、diff 解析、message 提取和符号关联留到后续 `HI-02` 及以后任务处理。
