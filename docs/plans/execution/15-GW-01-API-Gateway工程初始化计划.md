# GW-01 API Gateway 工程初始化计划 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `backend/api-gateway` 从 Maven 模块骨架补齐为可独立启动、带鉴权占位和最小测试闭环的 Spring Boot API Gateway 服务。

**Architecture:** 在现有 `backend/api-gateway` 模块中引入正式的 `Spring Boot + Spring Web MVC + Spring Security` 技术栈，先固化统一入口、健康检查、鉴权占位、未认证 JSON 返回和日志审计占位，再为后续真实 JWT/OIDC、服务转发和权限控制预留扩展空间。本轮坚持最小可运行边界，不接入任何真实身份源和下游代理。

**Tech Stack:** `Java 21`、`Spring Boot 3.3.13`、`Spring Security`、`Spring MVC`、`JUnit 5`、`MockMvc`

---

## 1. 任务边界

- 范围内：
  - 补齐 `api-gateway` 启动类与基础包结构
  - 引入 Web、Security、Actuator、Validation、Test 依赖
  - 提供 `/api/healthz` 和 `/api/auth/me`
  - 提供 `Bearer dev-token` 鉴权占位
  - 提供未认证统一 `401` JSON
  - 提供请求审计日志占位
  - 补齐模块测试与整仓构建验证
  - 回写当前任务状态和进度文档
- 范围外：
  - 不接入真实 JWT / OIDC
  - 不实现限流
  - 不实现项目级权限控制
  - 不实现下游服务路由转发
  - 不接数据库审计表

## 2. 验证方式

- `mvn -q -pl api-gateway test`
- `mvn -q -DskipTests package`
- 人工核对：
  - `/api/healthz` 匿名可访问
  - `/api/auth/me` 未认证返回 `401`
  - `/api/auth/me` 带 `Bearer dev-token` 返回开发态用户信息

## 3. 回写目标

- `docs/context/current/02-当前任务状态.md`
- `docs/context/progress/2026-05-28-GW-01-API-Gateway工程初始化进度.md`
- 必要时更新 `README.md`

### Task 1: 补齐 `api-gateway` 模块依赖与启动入口
**Files:**
- Modify: `backend/api-gateway/pom.xml`
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/ApiGatewayApplication.java`

- [ ] **Step 1: 为模块补齐正式依赖**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- [ ] **Step 2: 补齐测试依赖**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: 增加 Spring Boot 启动类**

```java
package com.autocode.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

### Task 2: 先写网关最小行为测试

**Files:**
- Create: `backend/api-gateway/src/test/java/com/autocode/gateway/web/ApiGatewaySecurityTest.java`

- [ ] **Step 1: 写健康检查匿名可访问的失败测试**

```java
@Test
void shouldAllowAnonymousAccessToHealthz() throws Exception {
    mockMvc.perform(get("/api/healthz"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("api-gateway"));
}
```

- [ ] **Step 2: 写未认证访问 `/api/auth/me` 返回 401 的失败测试**

```java
@Test
void shouldReturn401WhenAuthMeWithoutToken() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
}
```

- [ ] **Step 3: 写 `dev-token` 通过认证的失败测试**

```java
@Test
void shouldReturnDevUserWhenBearerDevTokenProvided() throws Exception {
    mockMvc.perform(get("/api/auth/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer dev-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("dev-user"))
        .andExpect(jsonPath("$.username").value("developer"))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
}
```

- [ ] **Step 4: 运行测试并确认先失败**

```powershell
mvn -q -pl api-gateway test
```

Expected:
- 测试失败，原因是启动类、控制器和安全链尚未实现

### Task 3: 实现最小控制器与统一错误响应

**Files:**
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/web/HealthController.java`
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/web/AuthController.java`
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/web/ErrorResponse.java`
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/security/GatewayAuthenticationEntryPoint.java`

- [ ] **Step 1: 实现 `/api/healthz` 控制器**

```java
@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/healthz")
    Map<String, String> healthz() {
        return Map.of("status", "UP", "service", "api-gateway");
    }
}
```

- [ ] **Step 2: 定义统一错误响应 DTO**

```java
public record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp
) {
}
```

- [ ] **Step 3: 实现未认证入口返回 JSON**

```java
@Component
public class GatewayAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
```

- [ ] **Step 4: 实现 `/api/auth/me` 控制器**

```java
@GetMapping("/auth/me")
Map<String, Object> currentUser(Authentication authentication) {
    return Map.of(
        "userId", "dev-user",
        "username", "developer",
        "roles", List.of("ROLE_USER")
    );
}
```

### Task 4: 实现鉴权占位与安全过滤链

**Files:**
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/security/DevPrincipal.java`
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/security/DevBearerAuthenticationFilter.java`
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/security/GatewaySecurityConfig.java`

- [ ] **Step 1: 定义开发态认证主体**

```java
public record DevPrincipal(
        String userId,
        String username,
        List<String> roles
) {
}
```

- [ ] **Step 2: 实现 Bearer dev-token 过滤器**

```java
if ("Bearer dev-token".equals(header)) {
    var principal = new DevPrincipal("dev-user", "developer", List.of("ROLE_USER"));
    var authentication = new UsernamePasswordAuthenticationToken(
        principal,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

- [ ] **Step 3: 配置无状态安全过滤链**

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/healthz", "/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll()
    );
```

- [ ] **Step 4: 将过滤器挂到用户名密码过滤器之前**

```java
http.addFilterBefore(devBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

### Task 5: 实现请求审计日志占位并让测试转绿

**Files:**
- Create: `backend/api-gateway/src/main/java/com/autocode/gateway/audit/RequestAuditFilter.java`

- [ ] **Step 1: 实现单次请求日志过滤器**

```java
long start = System.currentTimeMillis();
filterChain.doFilter(request, response);
long duration = System.currentTimeMillis() - start;
```

- [ ] **Step 2: 记录方法、路径、状态、耗时和主体**

```java
log.info("gateway_request method={} path={} status={} durationMs={} principal={}",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        duration,
        principal);
```

- [ ] **Step 3: 运行模块测试并确认通过**

```powershell
mvn -q -pl api-gateway test
```

Expected:
- 3 个最小行为测试全部通过

### Task 6: 整仓构建验证并回写状态

**Files:**
- Modify: `docs/context/current/02-当前任务状态.md`
- Create: `docs/context/progress/2026-05-28-GW-01-API-Gateway工程初始化进度.md`
- Modify: `README.md`

- [ ] **Step 1: 运行整仓构建验证**

```powershell
mvn -q -DskipTests package
```

Expected:
- 后端聚合工程构建通过

- [ ] **Step 2: 回写当前任务状态**

```markdown
- 已完成 `GW-01` API Gateway 工程初始化
- 已具备统一入口、鉴权占位和最小测试闭环
- 下一步进入 `PS-01` Project Service 工程初始化
```

- [ ] **Step 3: 记录本轮进度**

```markdown
- 新增 Spring Boot 启动类
- 新增安全过滤链和开发态 Token 占位
- 新增网关最小接口与测试
```

## 4. 自检

- `GW-01` 范围与设计 spec 一致：是
- 正式使用 `Spring Security` 而非临时拦截器：是
- 先写测试并验证失败，再补实现：是
- 验证口径覆盖模块测试与整仓构建：是
