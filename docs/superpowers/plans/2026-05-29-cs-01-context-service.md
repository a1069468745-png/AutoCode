# CS-01 Context Service Minimal Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the minimum runnable `context-service` Spring Boot skeleton with a unified success response model, health/readiness endpoints, basic Redis readiness probing, and tests.

**Architecture:** Keep `CS-01` at the same initialization depth as `KI-01` and `HI-01`: one Spring Boot entrypoint, one focused response model, one controller for system endpoints, and one narrow Redis probe service. Leave query orchestration, adapter integration, caching policy, and business APIs out of scope so this task only establishes a safe and testable service boundary.

**Tech Stack:** Java 21, Spring Boot 3.3, Maven, Spring Web, Spring Data Redis, JUnit 5, AssertJ, MockMvc, Mockito

---

## File Map

- Create: `backend/context-service/src/main/java/com/autocode/context/ContextServiceApplication.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/api/ApiResponse.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/health/ContextHealthController.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/health/ContextReadiness.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/health/ContextReadinessService.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/redis/ContextRedisProbe.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/redis/ContextRedisConfiguration.java`
- Create: `backend/context-service/src/test/java/com/autocode/context/ContextServiceApplicationTest.java`
- Create: `backend/context-service/src/test/java/com/autocode/context/health/ContextHealthControllerTest.java`
- Create: `backend/context-service/src/test/java/com/autocode/context/redis/ContextRedisProbeTest.java`
- Modify: `backend/context-service/pom.xml`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Create: `docs/context/progress/2026-05-29-CS-01-Context-Service工程初始化进度.md`
- Modify: `README.md`

### Task 1: Add failing tests for startup, system endpoints, and Redis readiness

**Files:**
- Modify: `backend/context-service/pom.xml`
- Create: `backend/context-service/src/test/java/com/autocode/context/ContextServiceApplicationTest.java`
- Create: `backend/context-service/src/test/java/com/autocode/context/health/ContextHealthControllerTest.java`
- Create: `backend/context-service/src/test/java/com/autocode/context/redis/ContextRedisProbeTest.java`

- [ ] **Step 1: Add the web, Redis, and test dependencies**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

- [ ] **Step 2: Write the failing application context test**

```java
package com.autocode.context;

import com.autocode.context.health.ContextReadinessService;
import com.autocode.context.redis.ContextRedisProbe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContextServiceApplicationTest {

    @Autowired
    private ContextReadinessService contextReadinessService;

    @Autowired
    private ContextRedisProbe contextRedisProbe;

    @Test
    void shouldLoadContextWithCoreBeans() {
        assertThat(contextReadinessService).isNotNull();
        assertThat(contextRedisProbe).isNotNull();
    }
}
```

- [ ] **Step 3: Write the failing endpoint tests**

```java
package com.autocode.context.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContextHealthController.class)
class ContextHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContextReadinessService contextReadinessService;

    @Test
    void shouldReturnHealthResponse() throws Exception {
        mockMvc.perform(get("/internal/context/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("context-service is healthy"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void shouldReturnReadinessResponseWhenRedisAvailable() throws Exception {
        given(contextReadinessService.check()).willReturn(new ContextReadiness("UP", true, "Redis probe succeeded"));

        mockMvc.perform(get("/internal/context/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.redisAvailable").value(true));
    }

    @Test
    void shouldReturn503ReadinessResponseWhenRedisUnavailable() throws Exception {
        given(contextReadinessService.check()).willReturn(new ContextReadiness("DOWN", false, "Redis probe failed"));

        mockMvc.perform(get("/internal/context/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("DOWN"))
                .andExpect(jsonPath("$.data.redisAvailable").value(false));
    }
}
```

- [ ] **Step 4: Write the failing Redis probe tests**

```java
package com.autocode.context.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextRedisProbeTest {

    @Test
    void shouldReturnTrueWhenProbeRoundTripSucceeds() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("autocode:context:probe")).thenReturn("ok");

        ContextRedisProbe probe = new ContextRedisProbe(redisTemplate);

        assertThat(probe.isAvailable()).isTrue();
        verify(valueOperations).set(eq("autocode:context:probe"), eq("ok"), eq(Duration.ofSeconds(5)));
    }

    @Test
    void shouldReturnFalseWhenProbeThrows() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("redis down"))
                .when(valueOperations)
                .set(eq("autocode:context:probe"), eq("ok"), eq(Duration.ofSeconds(5)));

        ContextRedisProbe probe = new ContextRedisProbe(redisTemplate);

        assertThat(probe.isAvailable()).isFalse();
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

Run: `mvn -pl context-service -am test`

Expected: FAIL because `ContextServiceApplication`, `ApiResponse`, `ContextHealthController`, `ContextReadiness`, `ContextReadinessService`, `ContextRedisProbe`, and `ContextRedisConfiguration` do not exist yet.

### Task 2: Implement the minimal service skeleton

**Files:**
- Create: `backend/context-service/src/main/java/com/autocode/context/ContextServiceApplication.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/api/ApiResponse.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/health/ContextHealthController.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/health/ContextReadiness.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/health/ContextReadinessService.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/redis/ContextRedisProbe.java`
- Create: `backend/context-service/src/main/java/com/autocode/context/redis/ContextRedisConfiguration.java`
- Modify: `backend/context-service/pom.xml`

- [ ] **Step 1: Create the Spring Boot entrypoint**

```java
package com.autocode.context;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContextServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContextServiceApplication.class, args);
    }
}
```

- [ ] **Step 2: Create the unified response model**

```java
package com.autocode.context.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, OffsetDateTime.now());
    }
}
```

- [ ] **Step 3: Create the readiness payload**

```java
package com.autocode.context.health;

public record ContextReadiness(
        String status,
        boolean redisAvailable,
        String detail
) {
}
```

- [ ] **Step 4: Create the Redis probe service**

```java
package com.autocode.context.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ContextRedisProbe {

    private static final String PROBE_KEY = "autocode:context:probe";
    private static final String PROBE_VALUE = "ok";
    private static final Duration PROBE_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;

    public ContextRedisProbe(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAvailable() {
        try {
            redisTemplate.opsForValue().set(PROBE_KEY, PROBE_VALUE, PROBE_TTL);
            return PROBE_VALUE.equals(redisTemplate.opsForValue().get(PROBE_KEY));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
```

- [ ] **Step 5: Create the readiness service**

```java
package com.autocode.context.health;

import com.autocode.context.redis.ContextRedisProbe;
import org.springframework.stereotype.Service;

@Service
public class ContextReadinessService {

    private final ContextRedisProbe contextRedisProbe;

    public ContextReadinessService(ContextRedisProbe contextRedisProbe) {
        this.contextRedisProbe = contextRedisProbe;
    }

    public ContextReadiness check() {
        boolean redisAvailable = contextRedisProbe.isAvailable();
        if (redisAvailable) {
            return new ContextReadiness("UP", true, "Redis probe succeeded");
        }
        return new ContextReadiness("DOWN", false, "Redis probe failed");
    }
}
```

- [ ] **Step 6: Create the health controller**

```java
package com.autocode.context.health;

import com.autocode.context.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/context")
public class ContextHealthController {

    private final ContextReadinessService contextReadinessService;

    public ContextHealthController(ContextReadinessService contextReadinessService) {
        this.contextReadinessService = contextReadinessService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok("context-service is healthy", Map.of("status", "UP"));
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiResponse<ContextReadiness>> readiness() {
        ContextReadiness readiness = contextReadinessService.check();
        HttpStatus httpStatus = "UP".equals(readiness.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.ok("context-service readiness checked", readiness));
    }
}
```

- [ ] **Step 7: Add the RedisTemplate configuration**

```java
package com.autocode.context.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ContextRedisConfiguration {

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
```

- [ ] **Step 8: Run the module tests**

Run: `mvn -pl context-service -am test`

Expected: PASS for the new application, controller, and Redis probe tests.

### Task 3: Tighten behavior and write back progress

**Files:**
- Modify: `backend/context-service/src/test/java/com/autocode/context/health/ContextHealthControllerTest.java`
- Modify: `backend/context-service/src/test/java/com/autocode/context/redis/ContextRedisProbeTest.java`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Create: `docs/context/progress/2026-05-29-CS-01-Context-Service工程初始化进度.md`
- Modify: `README.md`

- [ ] **Step 1: Add one more passing test for response timestamp presence**

```java
@Test
void shouldIncludeTimestampInHealthResponse() throws Exception {
    mockMvc.perform(get("/internal/context/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
}
```

- [ ] **Step 2: Add one more passing test for false read-back probe result**

```java
@Test
void shouldReturnFalseWhenProbeReadBackDiffers() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("autocode:context:probe")).thenReturn("unexpected");

    ContextRedisProbe probe = new ContextRedisProbe(redisTemplate);

    assertThat(probe.isAvailable()).isFalse();
}
```

- [ ] **Step 3: Re-run the focused module tests**

Run: `mvn -pl context-service -am test`

Expected: PASS with all `context-service` tests green.

- [ ] **Step 4: Write the progress note**

```markdown
# 2026-05-29 CS-01 Context Service 工程初始化进度

## 本次目标

按既定顺序启动 `CS-01`，让 `backend/context-service` 从仅有 Maven 模块和配置文件的占位骨架，演进为可独立启动、具备统一成功响应结构、健康检查与 Redis readiness 探针的 Spring Boot 服务骨架。

## 已完成内容

- 补齐 `context-service` Spring Boot 启动入口
- 落地统一成功响应模型 `ApiResponse`
- 落地 `/internal/context/health` 与 `/internal/context/readiness` 系统端点
- 落地最小 Redis 探针与 readiness 聚合服务
- 补齐上下文加载、系统端点与 Redis 探针测试

## 验证结果

- `mvn -pl context-service -am test` 通过

## 结论

`CS-01` 已完成，下一顺位继续推进 `CS-02`
```

- [ ] **Step 5: Update the current task status**

```markdown
- 已完成 `CS-01` Context Service 工程初始化
- 已落地 Spring Boot 启动入口、统一成功响应结构、系统健康检查与 Redis readiness 探针
- 已完成 `mvn -pl context-service -am test` 验证
```

- [ ] **Step 6: Update the current task index**

```markdown
- 进展：`CS-01` 已完成，下一顺位任务切换为 `CS-02`
```

- [ ] **Step 7: Update the README module status**

```markdown
- `backend/context-service`: 已完成 `CS-01` 最小工程初始化，具备统一成功响应、健康检查与 Redis readiness 骨架
```

- [ ] **Step 8: Commit**

```bash
git add backend/context-service docs/context/progress/2026-05-29-CS-01-Context-Service工程初始化进度.md docs/context/current/02-当前任务状态.md docs/indexes/03-当前任务索引.md README.md docs/superpowers/plans/2026-05-29-cs-01-context-service.md docs/superpowers/specs/2026-05-29-cs-01-context-service-minimal-design.md
git commit -m "feat: initialize context service skeleton"
```

## Self-Review

- Spec coverage: startup entrypoint, unified success response, system endpoints, Redis readiness probe, tests, verification, and writeback all map to Task 1-3.
- Placeholder scan: no `TODO`, `TBD`, or vague “handle later” steps remain.
- Type consistency: `ApiResponse`, `ContextHealthController`, `ContextReadiness`, `ContextReadinessService`, and `ContextRedisProbe` names are consistent across tests and implementation.
