# 2026-05-28 HI-01 History Indexer 工程初始化进度
## 本次目标

按既定顺序完成 `HI-01`，让 `backend/history-indexer` 从仅有 Maven 模块和配置文件的空骨架，演进为可独立启动、具备最小 Git 工作区准备与扫描范围校验能力、并带有基础自动化测试的 Spring Boot 服务骨架。

## 已完成内容

- 补齐 `history-indexer` 的 Spring Boot 启动入口
- 新增 `autocode.history` 配置属性，固化工作区根路径、Git 命令路径、默认分支和最大提交窗口基线
- 落地受控 Git 工作区准备服务，按 `projectKey/branchName` 生成运行目录
- 为分支名中的路径分隔符做归一化处理，避免生成不稳定的多级目录
- 增加对 `..` 越界片段的拒绝逻辑，防止工作目录逃逸出配置根路径
- 新增 `HistoryScanScope`，固化最小扫描边界校验规则
- 补齐应用上下文加载测试、扫描范围测试和工作区服务测试
- 为模块补齐 `spring-boot-starter-test` 与 Spring Boot 打包插件配置

## 关键实现

- 启动类：[backend/history-indexer/src/main/java/com/autocode/history/HistoryIndexerApplication.java](D:/project/AutoCode/backend/history-indexer/src/main/java/com/autocode/history/HistoryIndexerApplication.java)
- 配置属性：[backend/history-indexer/src/main/java/com/autocode/history/config/HistoryIndexerProperties.java](D:/project/AutoCode/backend/history-indexer/src/main/java/com/autocode/history/config/HistoryIndexerProperties.java)
- 范围对象：[backend/history-indexer/src/main/java/com/autocode/history/scope/HistoryScanScope.java](D:/project/AutoCode/backend/history-indexer/src/main/java/com/autocode/history/scope/HistoryScanScope.java)
- 工作区服务：[backend/history-indexer/src/main/java/com/autocode/history/workspace/HistoryWorkspaceService.java](D:/project/AutoCode/backend/history-indexer/src/main/java/com/autocode/history/workspace/HistoryWorkspaceService.java)
- 模块配置：[backend/history-indexer/src/main/resources/application.yml](D:/project/AutoCode/backend/history-indexer/src/main/resources/application.yml)
- 上下文测试：[backend/history-indexer/src/test/java/com/autocode/history/HistoryIndexerApplicationTest.java](D:/project/AutoCode/backend/history-indexer/src/test/java/com/autocode/history/HistoryIndexerApplicationTest.java)
- 范围测试：[backend/history-indexer/src/test/java/com/autocode/history/scope/HistoryScanScopeTest.java](D:/project/AutoCode/backend/history-indexer/src/test/java/com/autocode/history/scope/HistoryScanScopeTest.java)
- 工作区测试：[backend/history-indexer/src/test/java/com/autocode/history/workspace/HistoryWorkspaceServiceTest.java](D:/project/AutoCode/backend/history-indexer/src/test/java/com/autocode/history/workspace/HistoryWorkspaceServiceTest.java)

## 验证结果

- `mvn -pl history-indexer -am test`：通过

## 当前结论

`HI-01` 已完成，当前仓库已具备 History Indexer 的最小可用工程入口、最小扫描范围约束和受控 Git 工作区准备基线，可以按顺序进入 `KI-01` Knowledge Indexer 工程初始化。
