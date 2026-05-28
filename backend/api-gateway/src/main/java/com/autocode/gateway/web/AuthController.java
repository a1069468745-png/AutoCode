package com.autocode.gateway.web;

import com.autocode.gateway.security.DevPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        DevPrincipal principal = (DevPrincipal) authentication.getPrincipal();
        return Map.of(
                "userId", principal.userId(),
                "username", principal.username(),
                "roles", principal.roles()
        );
    }
}
