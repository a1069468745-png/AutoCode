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
