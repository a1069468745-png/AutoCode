# 2026-05-29 CS-01 Context Service 最小初始化设计

## 1. 背景

当前仓库已完成 `GW-01`、`PS-01`、`CG-01`、`HI-01` 与 `KI-01` 的最小工程初始化，当前顺位已切换到 `CS-01`。
`backend/context-service` 目前只有 `pom.xml` 与 `application.yml` 占位，尚不具备可独立启动的 Spring Boot 入口、统一响应模型、健康检查端点、Redis 连通能力与最小自动化测试。

结合 `docs/plans/execution/06-开发任务拆分与执行计划.md` 对 `CS-01` 的任务边界，本轮只完成 Context Service 的最小工程初始化，目标是建立统一查询编排服务后续演进所需的稳定服务骨架，而不是提前实现任何业务查询能力。

## 2. 目标

本轮目标是让 `context-service` 从空壳模块演进为可启动、可验证、可测试的最小 Spring Boot 服务骨架，具体包括：

- 补齐 Spring Boot 启动入口
- 建立统一响应结构，作为后续查询接口的固定出参基础
- 提供最小健康检查与 readiness 端点
- 建立基础 Redis 连通能力与受控的探针式验证
- 补齐上下文加载、端点调用与 Redis 连通相关的最小测试

## 3. 非目标

本轮明确不做以下内容：

- 不实现任何查询编排、意图识别或上下文聚合逻辑
- 不接入 CodeGraph、History、Knowledge 三类索引查询适配器
- 不引入鉴权、限流、审计等网关后置能力
- 不实现缓存键策略、查询缓存、失效策略或预热逻辑
- 不对外暴露真实业务接口，只保留骨架级别的系统端点

## 4. 方案比较

### 方案 A：只补启动类和健康检查

- 内容：仅增加 Spring Boot 启动入口和最小健康检查
- 优点：工作量最小
- 缺点：无法覆盖 `CS-01` 计划里提到的统一响应结构和基础缓存接入，后续仍需回头补基础边界

### 方案 B：最小服务骨架

- 内容：补启动类、统一响应模型、健康检查控制器、Redis 探针服务和测试
- 优点：既保持范围克制，又把后续 `CS-*` 必然依赖的服务基础设施一次立住
- 缺点：比纯启动类多一层响应与 Redis 抽象设计

### 方案 C：最小服务骨架加业务占位接口

- 内容：在方案 B 基础上，再增加 `/api/context/query` 等业务占位接口
- 优点：更贴近后续真实形态
- 缺点：容易提前触碰 `CS-02+` 的请求模型、查询分类和聚合语义，超出 `CS-01` 边界

## 5. 选型

本轮采用方案 B。

原因如下：

- 它与 `CG-01`、`HI-01`、`KI-01` 的初始化深度一致，仍属于“可启动、可测试、边界清晰”的骨架建设
- 它覆盖了 `CS-01` 已明确要求的统一响应结构与基础缓存接入
- 它不会提前把查询编排、适配器抽象和请求模型带入当前任务

## 6. 设计

### 6.1 模块结构

本轮新增以下最小结构：

- `com.autocode.context.ContextServiceApplication`
- `com.autocode.context.api.ApiResponse`
- `com.autocode.context.health.ContextHealthController`
- `com.autocode.context.health.ContextReadinessService`
- `com.autocode.context.redis.ContextRedisProbe`
- `com.autocode.context.redis.ContextRedisConfiguration`

其中：

- `ContextServiceApplication` 负责模块启动
- `ApiResponse` 负责统一承载成功响应结构，固定 `success / code / message / data / timestamp`
- `ContextHealthController` 负责暴露系统级健康检查与 readiness 端点
- `ContextReadinessService` 负责聚合服务自身与 Redis 的就绪状态
- `ContextRedisProbe` 负责最小化 Redis 读写探针，不承担缓存策略职责
- `ContextRedisConfiguration` 负责提供当前模块所需的 RedisTemplate 或等价最小 Bean 配置

### 6.2 统一响应结构

本轮先只固化成功响应结构，错误模型与全局异常处理留给后续任务。

建议统一响应字段如下：

- `success`：布尔值，`CS-01` 中固定为 `true`
- `code`：字符串，成功场景默认 `OK`
- `message`：字符串，成功场景返回简短说明
- `data`：泛型载荷
- `timestamp`：服务端生成的 ISO-8601 时间戳

这样可以先把外部接口的响应骨架统一下来，后续 `CS-*` 只需在此基础上扩展错误码和异常语义。

### 6.3 端点设计

本轮只提供两个系统端点：

- `GET /internal/context/health`
- `GET /internal/context/readiness`

约束如下：

- `health` 只表示服务进程本身可响应，不依赖 Redis 探针结果
- `readiness` 表示当前服务已具备后续接入查询能力的基础依赖，其中 Redis 必须可连通
- 两个端点均使用统一响应结构返回
- 不在本轮引入 Spring Boot Actuator 以外的复杂指标体系；若现有模块未统一启用 Actuator，则直接使用普通 Controller 即可

### 6.4 Redis 探针规则

本轮只建立最小探针能力，不做缓存抽象。

规则如下：

- 使用现有 `spring.data.redis.*` 配置连接 Redis
- readiness 检查时执行一次最小探针，例如写入短生命周期键再读回
- 探针键必须使用明显的系统前缀，例如 `autocode:context:probe`
- 探针失败时返回明确失败状态，但不吞掉异常语义
- 不在业务路径中复用该探针逻辑，避免把健康检查代码误当缓存基础设施

### 6.5 测试设计

本轮按最小 TDD 闭环补三类测试：

- 应用上下文测试：验证 Spring Boot 上下文可启动，并能注入关键 Bean
- 端点测试：验证 `health` 与 `readiness` 端点返回统一响应结构
- Redis 探针测试：验证 Redis 不可用时 readiness 明确失败；可用场景优先通过 mock 或最小桩对象验证探针逻辑，不要求本轮引入容器化集成测试

### 6.6 依赖策略

本轮依赖以最小够用为原则：

- 保留 `spring-boot-starter`
- 增加 `spring-boot-starter-web`
- 增加 `spring-boot-starter-data-redis`
- 增加 `spring-boot-starter-test`

除上述依赖外，不引入额外缓存框架、序列化框架或服务发现组件。

## 7. 验证方式

本轮完成判据：

- `context-service` 可以通过标准入口启动
- `GET /internal/context/health` 返回成功结构
- `GET /internal/context/readiness` 能区分 Redis 可用与不可用
- `mvn -pl context-service -am test` 通过
- 没有引入任何超出 `CS-01` 范围的业务查询接口或适配器代码

## 8. 风险与控制

- 风险：把 readiness 探针扩展成缓存基础设施
  - 控制：限制探针职责，仅允许最小读写验证
- 风险：为了“统一响应”提前设计过重的错误模型
  - 控制：本轮只固化成功结构，错误结构后置
- 风险：把业务查询接口提前塞进 `CS-01`
  - 控制：本轮端点只允许系统级健康与 readiness 检查

## 9. 结论

`CS-01` 应交付一个最小可运行的 Context Service 服务骨架：可启动、可返回统一成功结构、可进行系统健康检查、可验证 Redis 基础可用性、可通过自动化测试。
查询编排、上下文聚合、适配器接入与缓存策略均留到后续 `CS-*` 任务处理。
