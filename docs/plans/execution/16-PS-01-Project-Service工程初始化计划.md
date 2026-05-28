# PS-01 Project Service 工程初始化计划 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `backend/project-service` 从 Maven 模块骨架补齐为可独立启动、可落库、可缓存项目元数据，并提供最小项目接口闭环的 Spring Boot 服务。

**Architecture:** 在现有 `backend/project-service` 模块内引入 `Spring Boot + Spring MVC + JDBC + Redis` 技术栈，围绕 `app.projects` 打通创建、列表、详情三条最小主链。数据以 PostgreSQL 为权威来源，Redis 仅承担项目元数据缓存，接口层通过统一异常结构暴露最小可用项目管理入口，为后续 `PS-02` 到 `PS-04` 与 `GW-*` 转发接入提供稳定底座。

**Tech Stack:** `Java 21`、`Spring Boot 3.3.13`、`Spring MVC`、`Spring JDBC`、`RedisTemplate`、`JUnit 5`、`MockMvc`、`Testcontainers`

---

## 1. 任务边界

- 范围内：
  - 补齐 `project-service` 启动类与基础包结构
  - 引入 Web、Validation、JDBC、Redis、Actuator、PostgreSQL 驱动与测试依赖
  - 提供 `POST /api/projects`
  - 提供 `GET /api/projects`
  - 提供 `GET /api/projects/{id}`
  - 接入 `app.projects` 的插入、列表、详情查询
  - 按 `DB-02` 规范提供项目列表和项目详情的最小元数据缓存
  - 提供统一 `400`、`404`、`409` JSON 错误结构
  - 补齐模块测试、整仓构建验证和状态文档回写
- 范围外：
  - 不实现更新、删除
  - 不实现 `/api/projects/{id}/index`
  - 不实现 `/api/projects/{id}/model-policy`
  - 不接入 `app.model_profiles`
  - 不实现成员、角色和权限映射
  - 不实现复杂搜索、分页、筛选与审批流

## 2. 验证方式

- `mvn -q -pl project-service -am test`
- `mvn -q -DskipTests package`
- 人工核对：
  - `POST /api/projects` 可创建项目并默认写入 `CREATED`
  - `GET /api/projects` 返回按 `id desc` 排序的项目列表
  - `GET /api/projects/{id}` 能命中数据库并可回填详情缓存
  - 重复项目名返回 `409`
  - 不存在项目返回 `404`

## 3. 回写目标

- `README.md`
- `docs/context/current/01-当前阶段与目标.md`
- `docs/context/current/02-当前任务状态.md`
- `docs/indexes/02-当前阶段索引.md`
- `docs/indexes/03-当前任务索引.md`
- `docs/context/progress/2026-05-28-PS-01-Project-Service工程初始化进度.md`

### Task 1: 补齐 `project-service` 模块依赖与启动入口

**Files:**
- Modify: `backend/project-service/pom.xml`
- Create: `backend/project-service/src/main/java/com/autocode/project/ProjectServiceApplication.java`

- [ ] **Step 1: 为模块补齐运行时依赖**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
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
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: 增加 Spring Boot 启动类**

```java
package com.autocode.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }
}
```

### Task 2: 先写 `POST /api/projects` 与错误结构的失败测试

**Files:**
- Create: `backend/project-service/src/test/java/com/autocode/project/web/ProjectControllerTest.java`

- [ ] **Step 1: 写创建项目成功的失败测试**

```java
@Test
void shouldCreateProject() throws Exception {
    mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "autocode-platform",
                  "repoUrl": "https://git.example.com/team/autocode-platform.git",
                  "defaultBranch": "main",
                  "languageStack": "Java,TypeScript",
                  "docRepoPath": "docs/"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("autocode-platform"))
        .andExpect(jsonPath("$.status").value("CREATED"));
}
```

- [ ] **Step 2: 写缺少必填字段返回 `400` 的失败测试**

```java
@Test
void shouldRejectCreateProjectWhenNameMissing() throws Exception {
    mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "repoUrl": "https://git.example.com/team/autocode-platform.git",
                  "defaultBranch": "main"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
}
```

- [ ] **Step 3: 写重复项目名返回 `409` 的失败测试**

```java
@Test
void shouldReturn409WhenProjectNameDuplicated() throws Exception {
    String payload = """
        {
          "name": "autocode-platform",
          "repoUrl": "https://git.example.com/team/autocode-platform.git",
          "defaultBranch": "main"
        }
        """;

    mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PROJECT_NAME_CONFLICT"));
}
```

- [ ] **Step 4: 运行测试并确认先失败**

```powershell
mvn -q -pl project-service -am test
```

Expected:
- 测试失败，原因是启动类、控制器、服务、仓储和容器化测试基座尚未实现

### Task 3: 再写列表、详情与缓存行为的失败测试

**Files:**
- Modify: `backend/project-service/src/test/java/com/autocode/project/web/ProjectControllerTest.java`

- [ ] **Step 1: 写查询项目列表成功的失败测试**

```java
@Test
void shouldListProjectsInDescendingIdOrder() throws Exception {
    createProject("autocode-core");
    createProject("autocode-web");

    mockMvc.perform(get("/api/projects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("autocode-web"))
        .andExpect(jsonPath("$[1].name").value("autocode-core"));
}
```

- [ ] **Step 2: 写查询项目详情成功的失败测试**

```java
@Test
void shouldGetProjectById() throws Exception {
    long projectId = createProject("autocode-platform");

    mockMvc.perform(get("/api/projects/{id}", projectId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(projectId))
        .andExpect(jsonPath("$.name").value("autocode-platform"))
        .andExpect(jsonPath("$.status").value("CREATED"));
}
```

- [ ] **Step 3: 写不存在项目返回 `404` 的失败测试**

```java
@Test
void shouldReturn404WhenProjectMissing() throws Exception {
    mockMvc.perform(get("/api/projects/{id}", 9999))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
}
```

- [ ] **Step 4: 写列表缓存失效与详情缓存回填的失败测试**

```java
@Test
void shouldEvictProjectListCacheAfterCreate() throws Exception {
    mockMvc.perform(get("/api/projects"))
        .andExpect(status().isOk());

    assertThat(redisTemplate.hasKey("ac:v1:project-meta:global:project:list")).isTrue();

    createProject("autocode-platform");

    assertThat(redisTemplate.hasKey("ac:v1:project-meta:global:project:list")).isFalse();
}

@Test
void shouldBackfillDetailCacheAfterGetById() throws Exception {
    long projectId = createProject("autocode-platform");

    mockMvc.perform(get("/api/projects/{id}", projectId))
        .andExpect(status().isOk());

    assertThat(redisTemplate.hasKey("ac:v1:project-meta:p:" + projectId + ":project:detail")).isTrue();
}
```

- [ ] **Step 5: 重新运行测试并确认继续失败**

```powershell
mvn -q -pl project-service -am test
```

Expected:
- 测试继续失败，但失败点收敛到未实现控制器与持久化逻辑

### Task 4: 搭建测试基座与容器化依赖

**Files:**
- Create: `backend/project-service/src/test/java/com/autocode/project/support/ProjectServiceIntegrationTestBase.java`
- Create: `backend/project-service/src/test/resources/application-test.yml`

- [ ] **Step 1: 编写 PostgreSQL 与 Redis Testcontainers 基座**

```java
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ProjectServiceIntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

- [ ] **Step 2: 在测试配置中关闭无关噪音并固定 TTL**

```yaml
spring:
  jackson:
    time-zone: UTC
management:
  endpoints:
    enabled-by-default: false
autocode:
  project:
    cache:
      detail-ttl: PT30M
      list-ttl: PT10M
```

- [ ] **Step 3: 在测试基座中初始化 `app.projects` 表**

```java
@BeforeEach
void resetSchema(@Autowired JdbcTemplate jdbcTemplate,
                 @Autowired StringRedisTemplate redisTemplate) {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    jdbcTemplate.execute("drop schema if exists app cascade");
    jdbcTemplate.execute("create schema app");
    jdbcTemplate.execute("""
        create table app.projects (
            id bigserial primary key,
            name varchar(128) not null,
            repo_url varchar(512) not null,
            default_branch varchar(128) not null,
            language_stack varchar(256),
            doc_repo_path varchar(512),
            status varchar(32) not null default 'CREATED',
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now(),
            constraint uq_projects_name unique (name),
            constraint chk_projects_status check (status in ('CREATED', 'INDEXING', 'READY', 'FAILED', 'DISABLED'))
        )
        """);
}
```

- [ ] **Step 4: 让 `ProjectControllerTest` 继承测试基座**

```java
class ProjectControllerTest extends ProjectServiceIntegrationTestBase {
}
```

### Task 5: 实现请求模型、响应模型与统一异常结构

**Files:**
- Create: `backend/project-service/src/main/java/com/autocode/project/web/CreateProjectRequest.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/web/ProjectSummaryResponse.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/web/ProjectDetailResponse.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/web/ApiErrorResponse.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/domain/ProjectNotFoundException.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/domain/ProjectNameConflictException.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/web/ProjectExceptionHandler.java`

- [ ] **Step 1: 定义创建请求模型**

```java
public record CreateProjectRequest(
        @NotBlank String name,
        @NotBlank String repoUrl,
        @NotBlank String defaultBranch,
        String languageStack,
        String docRepoPath
) {
}
```

- [ ] **Step 2: 定义列表与详情响应模型**

```java
public record ProjectSummaryResponse(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String status
) {
}

public record ProjectDetailResponse(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String languageStack,
        String docRepoPath,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
```

- [ ] **Step 3: 定义统一错误结构与业务异常**

```java
public record ApiErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp
) {
}
```

```java
public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(long projectId) {
        super("Project " + projectId + " does not exist");
    }
}
```

```java
public class ProjectNameConflictException extends RuntimeException {
    public ProjectNameConflictException(String projectName) {
        super("Project name already exists: " + projectName);
    }
}
```

- [ ] **Step 4: 实现统一异常处理器**

```java
@RestControllerAdvice
public class ProjectExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleValidation(HttpServletRequest request) {
        return new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed",
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handleNotFound(ProjectNotFoundException ex, HttpServletRequest request) {
        return new ApiErrorResponse("PROJECT_NOT_FOUND", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(ProjectNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiErrorResponse handleConflict(ProjectNameConflictException ex, HttpServletRequest request) {
        return new ApiErrorResponse("PROJECT_NAME_CONFLICT", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }
}
```

### Task 6: 实现仓储层并跑通 PostgreSQL 读写

**Files:**
- Create: `backend/project-service/src/main/java/com/autocode/project/domain/ProjectRecord.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/domain/ProjectRepository.java`

- [ ] **Step 1: 定义项目记录模型**

```java
public record ProjectRecord(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String languageStack,
        String docRepoPath,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
```

- [ ] **Step 2: 实现插入方法**

```java
public ProjectRecord insert(CreateProjectRequest request) {
    return jdbcClient.sql("""
            insert into app.projects (
                name,
                repo_url,
                default_branch,
                language_stack,
                doc_repo_path,
                status
            ) values (
                :name,
                :repoUrl,
                :defaultBranch,
                :languageStack,
                :docRepoPath,
                'CREATED'
            )
            returning id, name, repo_url, default_branch, language_stack,
                      doc_repo_path, status, created_at, updated_at
            """)
        .param("name", request.name())
        .param("repoUrl", request.repoUrl())
        .param("defaultBranch", request.defaultBranch())
        .param("languageStack", request.languageStack())
        .param("docRepoPath", request.docRepoPath())
        .query(this::mapProject)
        .single();
}
```

- [ ] **Step 3: 实现列表与详情查询**

```java
public List<ProjectRecord> findAll() {
    return jdbcClient.sql("""
            select id, name, repo_url, default_branch, language_stack,
                   doc_repo_path, status, created_at, updated_at
            from app.projects
            order by id desc
            """)
        .query(this::mapProject)
        .list();
}

public Optional<ProjectRecord> findById(long id) {
    return jdbcClient.sql("""
            select id, name, repo_url, default_branch, language_stack,
                   doc_repo_path, status, created_at, updated_at
            from app.projects
            where id = :id
            """)
        .param("id", id)
        .query(this::mapProject)
        .optional();
}
```

- [ ] **Step 4: 将唯一键冲突翻译为领域异常**

```java
catch (DuplicateKeyException ex) {
    throw new ProjectNameConflictException(request.name());
}
```

### Task 7: 实现缓存仓储与缓存配置

**Files:**
- Create: `backend/project-service/src/main/java/com/autocode/project/config/ProjectCacheProperties.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/config/RedisConfig.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/domain/ProjectCacheRepository.java`
- Modify: `backend/project-service/src/main/resources/application.yml`

- [ ] **Step 1: 定义缓存配置属性**

```java
@ConfigurationProperties(prefix = "autocode.project.cache")
public record ProjectCacheProperties(
        Duration detailTtl,
        Duration listTtl
) {
}
```

- [ ] **Step 2: 配置字符串 RedisTemplate**

```java
@Configuration
@EnableConfigurationProperties(ProjectCacheProperties.class)
public class RedisConfig {

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

- [ ] **Step 3: 实现缓存仓储**

```java
public Optional<String> getProjectDetail(long projectId) {
    return Optional.ofNullable(redisTemplate.opsForValue()
            .get("ac:v1:project-meta:p:" + projectId + ":project:detail"));
}

public void putProjectDetail(long projectId, String payload) {
    redisTemplate.opsForValue().set(
        "ac:v1:project-meta:p:" + projectId + ":project:detail",
        payload,
        properties.detailTtl()
    );
}

public void evictProjectList() {
    redisTemplate.delete("ac:v1:project-meta:global:project:list");
}
```

- [ ] **Step 4: 在 `application.yml` 中补齐缓存 TTL**

```yaml
autocode:
  project:
    cache:
      detail-ttl: ${PROJECT_DETAIL_CACHE_TTL:PT30M}
      list-ttl: ${PROJECT_LIST_CACHE_TTL:PT10M}
```

### Task 8: 实现服务层、控制器与缓存编排

**Files:**
- Create: `backend/project-service/src/main/java/com/autocode/project/domain/ProjectService.java`
- Create: `backend/project-service/src/main/java/com/autocode/project/web/ProjectController.java`

- [ ] **Step 1: 实现服务层创建逻辑**

```java
public ProjectDetailResponse createProject(CreateProjectRequest request) {
    ProjectRecord created = repository.insert(request);
    cacheRepository.evictProjectList();
    return toDetailResponse(created);
}
```

- [ ] **Step 2: 实现服务层列表逻辑**

```java
public List<ProjectSummaryResponse> listProjects() {
    return cacheRepository.getProjectList()
        .map(this::readProjectListFromCache)
        .orElseGet(() -> {
            List<ProjectSummaryResponse> projects = repository.findAll().stream()
                .map(this::toSummaryResponse)
                .toList();
            cacheRepository.putProjectList(writeProjectListToCache(projects));
            return projects;
        });
}
```

- [ ] **Step 3: 实现服务层详情逻辑**

```java
public ProjectDetailResponse getProject(long projectId) {
    return cacheRepository.getProjectDetail(projectId)
        .map(this::readProjectDetailFromCache)
        .orElseGet(() -> {
            ProjectRecord record = repository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
            ProjectDetailResponse response = toDetailResponse(record);
            cacheRepository.putProjectDetail(projectId, writeProjectDetailToCache(response));
            return response;
        });
}
```

- [ ] **Step 4: 实现控制器接口**

```java
@RestController
@RequestMapping("/api/projects")
@Validated
public class ProjectController {

    @PostMapping
    ProjectDetailResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping
    List<ProjectSummaryResponse> listProjects() {
        return projectService.listProjects();
    }

    @GetMapping("/{id}")
    ProjectDetailResponse getProject(@PathVariable long id) {
        return projectService.getProject(id);
    }
}
```

- [ ] **Step 5: 运行测试并确认转绿**

```powershell
mvn -q -pl project-service -am test
```

Expected:
- 所有 `project-service` 测试通过
- 控制器、缓存与 PostgreSQL/Redis 集成行为满足 spec

### Task 9: 执行整仓构建验证并补充状态回写

**Files:**
- Modify: `README.md`
- Modify: `docs/context/current/01-当前阶段与目标.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/02-当前阶段索引.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Create: `docs/context/progress/2026-05-28-PS-01-Project-Service工程初始化进度.md`

- [ ] **Step 1: 运行整仓构建验证**

```powershell
mvn -q -DskipTests package
```

Expected:
- `backend` 多模块构建通过

- [ ] **Step 2: 回写阶段状态**

```markdown
- `PS-01` 已完成：`project-service` 已具备最小项目创建、列表、详情能力
- 当前顺序切换到后续索引服务初始化任务
```

- [ ] **Step 3: 补进度记录**

```markdown
## 已完成内容
- 新增 `project-service` 启动类、控制器、服务、仓储、缓存组件
- 打通 `app.projects` 读写闭环
- 新增 PostgreSQL + Redis 集成测试

## 验证结果
- `mvn -q -pl project-service -am test` 通过
- `mvn -q -DskipTests package` 通过
```

- [ ] **Step 4: 自查下一阶段入口**

```markdown
- 确认 `PS-01` 状态已同步到 README、当前阶段和任务索引
- 明确下一顺序任务并在状态文档中写明
```
