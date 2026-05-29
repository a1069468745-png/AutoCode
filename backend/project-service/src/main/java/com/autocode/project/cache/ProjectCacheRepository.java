package com.autocode.project.cache;

import com.autocode.project.config.ProjectCacheProperties;
import com.autocode.project.web.dto.ProjectDetailResponse;
import com.autocode.project.web.dto.ProjectSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
public class ProjectCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(ProjectCacheRepository.class);
    public static final String PROJECT_LIST_KEY = "ac:v1:project-meta:global:project:list";

    private static final String DETAIL_KEY_PATTERN = "ac:v1:project-meta:p:%d:project:detail";
    private static final TypeReference<List<ProjectSummaryResponse>> PROJECT_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration detailTtl;
    private final Duration listTtl;

    public ProjectCacheRepository(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  ProjectCacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.detailTtl = cacheProperties.detailTtl();
        this.listTtl = cacheProperties.listTtl();
    }

    public Optional<List<ProjectSummaryResponse>> findProjectList() {
        return read(PROJECT_LIST_KEY, PROJECT_LIST_TYPE);
    }

    public void cacheProjectList(List<ProjectSummaryResponse> projects) {
        write(PROJECT_LIST_KEY, projects, listTtl);
    }

    public void evictProjectList() {
        try {
            redisTemplate.delete(PROJECT_LIST_KEY);
        } catch (RuntimeException exception) {
            log.warn("Failed to evict project list cache", exception);
        }
    }

    public Optional<ProjectDetailResponse> findProjectDetail(long projectId) {
        return read(detailKey(projectId), ProjectDetailResponse.class);
    }

    public void cacheProjectDetail(ProjectDetailResponse projectDetail) {
        write(detailKey(projectDetail.id()), projectDetail, detailTtl);
    }

    public void evictProjectDetail(long projectId) {
        try {
            redisTemplate.delete(detailKey(projectId));
        } catch (RuntimeException exception) {
            log.warn("Failed to evict project detail cache for {}", projectId, exception);
        }
    }

    private String detailKey(long projectId) {
        return DETAIL_KEY_PATTERN.formatted(projectId);
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        String value;
        try {
            value = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to read cache key {}", key, exception);
            return Optional.empty();
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException exception) {
            safeDelete(key, exception);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("Failed to deserialize cache key {}", key, exception);
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(String key, TypeReference<T> type) {
        String value;
        try {
            value = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to read cache key {}", key, exception);
            return Optional.empty();
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException exception) {
            safeDelete(key, exception);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("Failed to deserialize cache key {}", key, exception);
            return Optional.empty();
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize cache payload for key {}", key, exception);
        } catch (RuntimeException exception) {
            log.warn("Failed to write cache key {}", key, exception);
        }
    }

    private void safeDelete(String key, Exception originalException) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException deleteException) {
            log.warn("Failed to delete corrupt cache key {}", key, deleteException);
        }
        log.warn("Failed to deserialize cache key {}", key, originalException);
    }
}
