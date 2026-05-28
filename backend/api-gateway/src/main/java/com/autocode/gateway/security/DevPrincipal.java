package com.autocode.gateway.security;

import java.util.List;

public record DevPrincipal(
        String userId,
        String username,
        List<String> roles
) {
}
