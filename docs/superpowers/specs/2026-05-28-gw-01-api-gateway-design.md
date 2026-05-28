# GW-01 API Gateway 工程初始化设计
## 1. 背景

当前仓库已经完成 `INF-01` 到 `INF-05` 基础设施与工程骨架阶段，并完成 `DB-01` PostgreSQL 核心表结构初始化和 `DB-02` Redis 键规范固化。按照既定顺序，下一步进入 `GW-01`，为前端与后续服务提供第一个正式可运行的在线服务入口。

根据 LLD，`api-gateway` 在一期 MVP 中承担以下职责：

- 统一对外 API 入口
- 登录鉴权
- 审计
- 请求路由

但当前仓库内 `backend/api-gateway` 仍只有 Maven 模块骨架和基础 `application.yml`，尚未具备实际可运行能力。因此 `GW-01` 的目标不是一次性做完整网关，而是先落成“可运行、可验证、带鉴权占位”的最小网关骨架。

## 2. 本轮目标

本轮只完成以下内容：

- 让 `backend/api-gateway` 成为可独立启动的 Spring Boot 服务
- 提供统一 `/api/**` 入口前缀下的最小接口
- 引入 Spring Security 过滤链并落地鉴权占位
- 提供未认证统一返回结构
- 提供基础审计日志占位
- 提供自动化测试，验证网关最小行为闭环

本轮不做以下内容：

- 不接入真实 JWT / OIDC
- 不对接 LDAP / AD / 企业统一认证
- 不实现限流
- 不实现项目级权限控制
- 不实现下游服务真实转发
- 不实现 API Key、模型调用或网关聚合逻辑

## 3. 设计原则

### 3.1 先跑通正式技术栈

即使当前只做占位，也直接采用 `Spring Boot + Spring Web MVC + Spring Security` 的正式栈，而不是先用轻量拦截器临时拼装，避免后续重复推翻过滤链。

### 3.2 先鉴权占位，再真实认证

本轮只验证“认证链是否存在且可工作”，不验证真实身份来源。因此使用固定开发态 Bearer Token 作为占位方案，为后续切换 JWT 或 OIDC 预留接口边界。

### 3.3 先入口统一，再下游分发

先固化统一入口、健康检查、认证链和统一错误结构，再进入后续服务转发。这样可以让前端、Nginx 和联调环境尽早对齐正式网关地址和行为。

### 3.4 先日志审计占位，再补持久化审计

本轮只把请求审计落到日志层，记录路径、方法、耗时和认证主体。等 `GW-*` 后续任务再决定是否接数据库或审计表。

## 4. 方案选择

### 方案 A：Spring MVC + Spring Security 可运行网关骨架

内容：

- 引入 `spring-boot-starter-web`
- 引入 `spring-boot-starter-security`
- 引入 `spring-boot-starter-actuator`
- 构建正式安全过滤链
- 提供最小控制器与测试

优点：

- 与 LLD 完全一致
- 后续接 JWT / OIDC 改造成本低
- 一开始就把过滤链、白名单、认证失败响应等骨架做对

缺点：

- 本轮实现量高于只做 Web 层骨架

### 方案 B：Web 层接口 + 自定义 HandlerInterceptor 鉴权占位

内容：

- 只引入 Web
- 用拦截器手动判断 Token
- 手写未认证响应

优点：

- 实现更快

缺点：

- 后续切入 Spring Security 时需要重做
- 过滤链和异常处理会出现二次迁移成本

### 结论

本轮选择方案 A。原因是用户已明确要求“直接带鉴权占位”，且仓库当前处于正式骨架搭建阶段，优先选择可延续到后续实现的正式安全链路更稳妥。

## 5. 组件设计

### 5.1 `ApiGatewayApplication`

职责：

- 作为 Spring Boot 启动入口
- 启动嵌入式容器
- 扫描 `api-gateway` 模块内配置、控制器和安全组件

输出：

- 可通过 `server.port` 启动服务

### 5.2 `GatewaySecurityConfig`

职责：

- 定义 Spring Security `SecurityFilterChain`
- 放行 `/api/healthz`
- 放行 `/actuator/health` 和 `/actuator/info`
- 要求其余 `/api/**` 请求必须认证
- 注册鉴权占位过滤器
- 配置统一 `AuthenticationEntryPoint`
- 禁用当前不需要的表单登录和 Session

关键约束：

- 使用无状态认证模式
- 未认证响应统一为 JSON

### 5.3 `DevBearerAuthenticationFilter`

职责：

- 读取请求头 `Authorization`
- 识别 `Bearer dev-token`
- 为合法开发态 Token 构造一个认证主体并写入 `SecurityContext`

行为约束：

- 未携带 Token 时不主动抛异常，交由安全链处理未认证响应
- Token 非 `dev-token` 时视为未认证

认证主体建议字段：

- `userId`：固定开发占位值，例如 `dev-user`
- `username`：固定开发占位值，例如 `developer`
- `roles`：固定角色集合，例如 `ROLE_USER`

本轮不引入：

- 数据库存储用户
- 第三方身份映射
- 项目级权限声明

### 5.4 `GatewayAuthenticationEntryPoint`

职责：

- 对所有未认证请求返回统一 `401` JSON

建议返回结构：

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication is required",
  "path": "/api/auth/me",
  "timestamp": "2026-05-28T12:00:00Z"
}
```

设计目的：

- 让前端在联调阶段就能按正式错误结构处理
- 避免直接返回空白 401 或默认 HTML 错误页

### 5.5 `HealthController`

职责：

- 提供 `GET /api/healthz`

建议返回结构：

```json
{
  "status": "UP",
  "service": "api-gateway"
}
```

用途：

- 前端或 Nginx 可快速验证网关是否启动
- 作为后续部署健康检查的业务层补充入口

### 5.6 `AuthController`

职责：

- 提供 `GET /api/auth/me`
- 返回当前认证主体的最小信息

建议返回结构：

```json
{
  "userId": "dev-user",
  "username": "developer",
  "roles": [
    "ROLE_USER"
  ]
}
```

用途：

- 验证鉴权占位是否生效
- 为前端后续接入“当前登录态”预留接口形态

### 5.7 `RequestAuditInterceptor` 或等价日志过滤器

职责：

- 记录请求方法、路径、响应状态、耗时、认证主体

日志目标：

- 本轮只落日志，不落数据库
- 日志格式应便于后续接 Loki / Grafana 或文件检索

建议字段：

- `method`
- `path`
- `status`
- `durationMs`
- `principal`

## 6. 接口范围

本轮只提供以下网关自有接口：

- `GET /api/healthz`
- `GET /api/auth/me`

访问规则：

- `/api/healthz`：匿名可访问
- `/actuator/health`：匿名可访问
- `/actuator/info`：匿名可访问
- `/api/auth/me`：必须认证

本轮不开放：

- `/api/projects/**`
- `/api/query/**`
- `/api/agent/**`

这些接口后续应通过网关统一路由到具体服务，但不属于 `GW-01` 范围。

## 7. 依赖与配置设计

### 7.1 Maven 依赖

`backend/api-gateway/pom.xml` 本轮应至少补齐：

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-actuator`
- `spring-boot-starter-validation`
- `spring-boot-starter-test`
- `spring-security-test`

### 7.2 配置约束

沿用现有 `application.yml`，并保持：

- `spring.application.name=api-gateway`
- `server.port=${API_GATEWAY_PORT:18080}`
- Actuator 对外暴露 `health,info`

本轮可增加的最小配置：

- 开发态 Token 配置项，例如 `autocode.gateway.dev-token`
- 日志级别配置

本轮不增加：

- JWT 公钥配置
- OIDC issuer 配置
- 路由表配置

## 8. 错误处理设计

本轮只统一处理认证失败错误。

行为要求：

- 未认证访问受保护接口时，返回 `401`
- 返回体为 JSON，不返回默认 HTML
- 返回体包含错误码、错误消息、请求路径和时间戳

本轮不处理：

- 403 权限不足
- 业务参数校验错误统一封装
- 下游服务异常透传

## 9. 测试与验证策略

### 9.1 自动化测试

最少覆盖以下 3 个场景：

1. `GET /api/healthz` 在未认证情况下返回 `200`
2. `GET /api/auth/me` 在未带 Token 时返回 `401`
3. `GET /api/auth/me` 携带 `Authorization: Bearer dev-token` 时返回 `200` 且包含固定开发用户信息

建议采用：

- `SpringBootTest` 或 `WebMvcTest`
- `MockMvc`
- `spring-security-test`

### 9.2 构建验证

本轮完成后应至少验证：

- `mvn -q -pl api-gateway test`
- `mvn -q -DskipTests package`

若网关引入依赖后影响整仓构建，也需要同步修复。

## 10. 风险与后续衔接

### 10.1 本轮主动不做的事情

- 不把 `dev-token` 设计成正式认证机制
- 不把日志审计误当成持久化审计完成
- 不提前做项目级权限模型
- 不提前做服务路由代理

### 10.2 这样做的好处

- 网关从占位容器切换为正式 Spring Boot 服务
- 前端与 Nginx 能尽早对齐正式入口
- 后续接入真实认证时不需要推翻整体框架
- 为 `PS-01`、`CS-*` 等服务接入前先把统一入口立住

## 11. 本轮实施输出

实施完成后至少应产出：

- `GW-01` 执行计划文档
- `api-gateway` 可运行 Spring Boot 代码
- 最小自动化测试
- 当前阶段与任务状态回写
- `GW-01` 进度记录
