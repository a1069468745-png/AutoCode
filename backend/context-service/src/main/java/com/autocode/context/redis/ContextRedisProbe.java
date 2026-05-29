package com.autocode.context.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

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
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valueOperations.set(PROBE_KEY, PROBE_VALUE, PROBE_TTL);
            return PROBE_VALUE.equals(valueOperations.get(PROBE_KEY));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
