package com.autocode.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "autocode.gateway.auth")
public record AuthProperties(
        String jwtSecret,
        long jwtExpirationSeconds,
        List<LocalUser> localUsers
) {
    public record LocalUser(
            String username,
            String password,
            String userId,
            List<String> roles
    ) {
    }
}
