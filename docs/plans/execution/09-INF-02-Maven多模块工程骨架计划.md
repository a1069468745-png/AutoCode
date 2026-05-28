# INF-02 Maven 多模块工程骨架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 `backend/` 聚合工程、父 `pom.xml`、`bom` 依赖管理模块和后端空模块 POM，使后端骨架可通过统一命令构建。

**Architecture:** `backend/pom.xml` 同时作为聚合与父工程，负责模块列表、统一属性和插件管理。`backend/bom` 负责集中管理 `Spring Boot 3.3+` 等依赖版本，其余模块只保留最小依赖声明，不实现业务逻辑。

**Tech Stack:** `Java 21`、`Maven 3.6.3`、`Spring Boot 3.3+`

---

## 1. 任务边界

- 范围内：
  - 创建 `backend/pom.xml`
  - 创建 `backend/bom/pom.xml`
  - 创建 `backend/common/pom.xml`
  - 创建 `api-gateway`、`project-service`、`context-service`、`codegraph-runner`、`history-indexer`、`knowledge-indexer`、`llm-gateway`、`dev-agent-service` 模块 `pom.xml`
- 范围外：
  - 不创建业务代码
  - 不创建 Spring Boot 启动类
  - 不创建配置文件和数据库访问代码

## 2. 验证方式

- `mvn -v` 确认本地 `Java 21`
- `mvn -q -DskipTests package` 验证聚合构建通过
- 人工检查各子模块是否未重复散落版本定义

## 3. 回写目标

- `docs/context/current/02-当前任务状态.md`
- `docs/context/progress/2026-05-28-INF-02-Maven多模块工程骨架进度.md`

### Task 1: 建立聚合父工程

**Files:**
- Create: `backend/pom.xml`

- [ ] **Step 1: 定义统一坐标、版本和模块列表**

```xml
<groupId>com.autocode</groupId>
<artifactId>backend-parent</artifactId>
<version>0.1.0-SNAPSHOT</version>
<packaging>pom</packaging>
```

- [ ] **Step 2: 固定统一属性**

```xml
<java.version>21</java.version>
<maven.compiler.release>21</maven.compiler.release>
<revision>0.1.0-SNAPSHOT</revision>
```

- [ ] **Step 3: 配置统一编译插件**

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.11.0</version>
  <configuration>
    <release>${maven.compiler.release}</release>
  </configuration>
</plugin>
```

### Task 2: 建立依赖版本管理模块

**Files:**
- Create: `backend/bom/pom.xml`

- [ ] **Step 1: 建立 `backend-bom` 模块**

```xml
<artifactId>backend-bom</artifactId>
<packaging>pom</packaging>
```

- [ ] **Step 2: 导入 Spring Boot 依赖 BOM**

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-dependencies</artifactId>
  <version>${spring-boot.version}</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

### Task 3: 建立公共模块与服务模块 POM

**Files:**
- Create: `backend/common/pom.xml`
- Create: `backend/api-gateway/pom.xml`
- Create: `backend/project-service/pom.xml`
- Create: `backend/context-service/pom.xml`
- Create: `backend/codegraph-runner/pom.xml`
- Create: `backend/history-indexer/pom.xml`
- Create: `backend/knowledge-indexer/pom.xml`
- Create: `backend/llm-gateway/pom.xml`
- Create: `backend/dev-agent-service/pom.xml`

- [ ] **Step 1: 统一继承 `backend-parent`**

```xml
<parent>
  <groupId>com.autocode</groupId>
  <artifactId>backend-parent</artifactId>
  <version>${revision}</version>
  <relativePath>../pom.xml</relativePath>
</parent>
```

- [ ] **Step 2: 统一导入内部 BOM**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.autocode</groupId>
      <artifactId>backend-bom</artifactId>
      <version>${revision}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

- [ ] **Step 3: 为服务模块预留基础依赖**

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
</dependency>
```

### Task 4: 验证并回写

**Files:**
- Modify: `docs/context/current/02-当前任务状态.md`
- Create: `docs/context/progress/2026-05-28-INF-02-Maven多模块工程骨架进度.md`

- [ ] **Step 1: 执行构建验证**

```powershell
mvn -q -DskipTests package
```

Expected:
- `backend` 聚合工程构建通过

- [ ] **Step 2: 回写任务状态**

```markdown
- 已完成 `INF-02` Maven 多模块工程骨架
- 后端模块目录与 POM 已建立
- 下一步进入 `INF-03` 前端工程骨架
```

## 4. 自检

- 与 `INF-02` 范围一致：是
- 未提前实现业务逻辑：是
- 验证命令明确：是
