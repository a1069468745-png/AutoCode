# 2026-05-28 PS-01 Project Service 工程初始化进度

## 本次目标

按既定顺序完成 `PS-01`，让 `backend/project-service` 从 Maven 模块骨架演进为可独立启动、可落库、可缓存项目元数据，并提供最小项目接口闭环的 Spring Boot 服务。

## 已完成内容

- 补齐 `project-service` 的 Spring Boot 启动入口和运行依赖
- 落地 `POST /api/projects`
- 落地 `GET /api/projects`
- 落地 `GET /api/projects/{id}`
- 接入 PostgreSQL `app.projects` 的插入、列表与详情查询
- 接入 Redis 项目列表与项目详情元数据缓存
- 补齐统一异常处理，覆盖 `VALIDATION_ERROR`、`PROJECT_NOT_FOUND`、`PROJECT_NAME_CONFLICT`
- 补齐模块级接口测试与缓存行为测试
- 将测试环境隔离到独立 PostgreSQL 测试库和 Redis logical DB，避免破坏共享本机数据

## 关键实现

- 启动类：[backend/project-service/src/main/java/com/autocode/project/ProjectServiceApplication.java](D:/project/AutoCode/backend/project-service/src/main/java/com/autocode/project/ProjectServiceApplication.java)
- 控制器：[backend/project-service/src/main/java/com/autocode/project/web/ProjectController.java](D:/project/AutoCode/backend/project-service/src/main/java/com/autocode/project/web/ProjectController.java)
- 服务编排：[backend/project-service/src/main/java/com/autocode/project/service/ProjectService.java](D:/project/AutoCode/backend/project-service/src/main/java/com/autocode/project/service/ProjectService.java)
- 仓储：[backend/project-service/src/main/java/com/autocode/project/domain/ProjectRepository.java](D:/project/AutoCode/backend/project-service/src/main/java/com/autocode/project/domain/ProjectRepository.java)
- 缓存仓储：[backend/project-service/src/main/java/com/autocode/project/cache/ProjectCacheRepository.java](D:/project/AutoCode/backend/project-service/src/main/java/com/autocode/project/cache/ProjectCacheRepository.java)
- 测试基座：[backend/project-service/src/test/java/com/autocode/project/support/ProjectServiceIntegrationTestBase.java](D:/project/AutoCode/backend/project-service/src/test/java/com/autocode/project/support/ProjectServiceIntegrationTestBase.java)

## 调试与修正记录

- 初始测试尝试使用 `Testcontainers`，但当前 Windows Docker Desktop 管道与 Java 侧连接不兼容，无法稳定启动容器化依赖。
- 测试基座随后改为复用本机 PostgreSQL 与 Redis，但第一次实现会误清共享数据；后续已收口到独立测试库 `autocode_project_service_test` 和 Redis logical DB `15`。
- 在 spec review 过程中，先后修正了缓存异常降级、创建响应字段收口、列表字段收口、详情响应时间字段补回等问题。
- 在 code quality review 过程中，进一步修正了测试隔离和 `DuplicateKeyException` 收窄映射问题，并补了超长字段校验测试。

## 验证结果

- `mvn -q -pl project-service -am test`：通过
- `mvn -q -DskipTests package`：通过

## 当前结论

`PS-01` 已完成，当前仓库已具备项目服务的最小可用主链，可以严格按顺序进入 `CG-01` CodeGraph Runner 工程初始化。
