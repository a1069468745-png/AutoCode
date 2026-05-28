# 2026-05-28 CG-01 CodeGraph Runner 工程初始化进度
## 本次目标

按既定顺序完成 `CG-01`，让 `backend/codegraph-runner` 从仅有 Maven 模块和配置文件的空骨架，演进为可独立启动、具备最小受控工作目录准备能力、并带有基础自动化测试的 Spring Boot 服务骨架。

## 已完成内容

- 补齐 `codegraph-runner` 的 Spring Boot 启动入口
- 新增 `autocode.codegraph` 配置属性，固化工作目录根路径、命令路径和命令超时基线
- 落地受控工作目录准备服务，按 `projectKey/branchName` 生成运行目录
- 为分支名中的路径分隔符做归一化处理，避免生成不稳定的多级目录
- 增加对 `..` 越界片段的拒绝逻辑，防止工作目录逃逸出配置根路径
- 补齐应用上下文加载测试和工作目录服务单元测试
- 为模块补齐 `spring-boot-starter-test` 与 Spring Boot 打包插件配置

## 关键实现

- 启动类：[backend/codegraph-runner/src/main/java/com/autocode/codegraph/CodegraphRunnerApplication.java](D:/project/AutoCode/backend/codegraph-runner/src/main/java/com/autocode/codegraph/CodegraphRunnerApplication.java)
- 配置属性：[backend/codegraph-runner/src/main/java/com/autocode/codegraph/config/CodegraphRunnerProperties.java](D:/project/AutoCode/backend/codegraph-runner/src/main/java/com/autocode/codegraph/config/CodegraphRunnerProperties.java)
- 工作目录服务：[backend/codegraph-runner/src/main/java/com/autocode/codegraph/workspace/RunnerWorkspaceService.java](D:/project/AutoCode/backend/codegraph-runner/src/main/java/com/autocode/codegraph/workspace/RunnerWorkspaceService.java)
- 模块配置：[backend/codegraph-runner/src/main/resources/application.yml](D:/project/AutoCode/backend/codegraph-runner/src/main/resources/application.yml)
- 上下文测试：[backend/codegraph-runner/src/test/java/com/autocode/codegraph/CodegraphRunnerApplicationTest.java](D:/project/AutoCode/backend/codegraph-runner/src/test/java/com/autocode/codegraph/CodegraphRunnerApplicationTest.java)
- 工作目录测试：[backend/codegraph-runner/src/test/java/com/autocode/codegraph/workspace/RunnerWorkspaceServiceTest.java](D:/project/AutoCode/backend/codegraph-runner/src/test/java/com/autocode/codegraph/workspace/RunnerWorkspaceServiceTest.java)

## 验证结果

- `mvn -pl codegraph-runner -am test`：通过

## 当前结论

`CG-01` 已完成，当前仓库已具备 CodeGraph Runner 的最小可用工程入口和受控工作目录准备基线，可以按顺序进入 `HI-01` History Indexer 工程初始化。
