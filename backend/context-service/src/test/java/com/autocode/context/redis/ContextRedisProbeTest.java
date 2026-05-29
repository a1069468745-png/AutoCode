package com.autocode.context.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextRedisProbeTest {
    private static final String PROBE_KEY = "autocode:context:probe";
    private static final String PROBE_VALUE = "ok";
    private static final Duration PROBE_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @Test
    void shouldReturnTrueWhenProbeWriteAndReadSucceed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(PROBE_KEY)).thenReturn(PROBE_VALUE);

        ContextRedisProbe probe = new ContextRedisProbe(redisTemplate);

        assertThat(probe.isAvailable()).isTrue();
        verify(valueOperations).set(PROBE_KEY, PROBE_VALUE, PROBE_TTL);
        verify(valueOperations).get(PROBE_KEY);
    }

    @Test
    void shouldReturnFalseWhenProbeWriteFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(valueOperations)
                .set(PROBE_KEY, PROBE_VALUE, PROBE_TTL);

        ContextRedisProbe probe = new ContextRedisProbe(redisTemplate);

        assertThat(probe.isAvailable()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenProbeReadBackDoesNotMatch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(PROBE_KEY)).thenReturn("stale");

        ContextRedisProbe probe = new ContextRedisProbe(redisTemplate);

        assertThat(probe.isAvailable()).isFalse();
        verify(valueOperations).set(PROBE_KEY, PROBE_VALUE, PROBE_TTL);
        verify(valueOperations).get(PROBE_KEY);
    }
}
