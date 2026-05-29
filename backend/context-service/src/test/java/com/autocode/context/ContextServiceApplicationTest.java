package com.autocode.context;

import com.autocode.context.health.ContextReadinessService;
import com.autocode.context.redis.ContextRedisProbe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ContextServiceApplication.class)
class ContextServiceApplicationTest {

    @Autowired
    private ContextReadinessService contextReadinessService;

    @Autowired
    private ContextRedisProbe contextRedisProbe;

    @Test
    void shouldLoadContextWithReadinessBeans() {
        assertThat(contextReadinessService).isNotNull();
        assertThat(contextRedisProbe).isNotNull();
    }
}
