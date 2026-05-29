package com.autocode.context.query.cache;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class QueryCacheService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public QueryCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<CachedQueryPayload> get(ContextQueryRequest request, QueryIntent intent) {
        String key = buildKey(request, intent);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, CachedQueryPayload.class));
        } catch (Exception ignored) {
            // Cache failure must not break the query pipeline.
            return Optional.empty();
        }
    }

    public void put(ContextQueryRequest request, QueryIntent intent, CachedQueryPayload payload) {
        String key = buildKey(request, intent);
        try {
            String value = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (JsonProcessingException ignored) {
            // Serialization failure degrades gracefully to non-cached behavior.
        }
    }

    private String buildKey(ContextQueryRequest request, QueryIntent intent) {
        String raw = (request.projectId() == null ? "0" : request.projectId()) + "|" +
                intent + "|" +
                (request.queryText() == null ? "" : request.queryText().trim().toLowerCase());
        return "context:query:cache:" + sha256(raw);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
