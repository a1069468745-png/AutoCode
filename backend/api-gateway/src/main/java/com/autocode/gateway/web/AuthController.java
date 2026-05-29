package com.autocode.gateway.web;

import com.autocode.gateway.security.AuthProperties;
import com.autocode.gateway.security.DevPrincipal;
import com.autocode.gateway.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenService tokenService;
    private final AuthProperties authProperties;

    public AuthController(TokenService tokenService, AuthProperties authProperties) {
        this.tokenService = tokenService;
        this.authProperties = authProperties;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }

        AuthProperties.LocalUser matchedUser = authProperties.localUsers().stream()
                .filter(u -> u.username().equals(username) && u.password().equals(password))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));

        String token = tokenService.generateToken(
                matchedUser.userId(),
                matchedUser.username(),
                String.join(",", matchedUser.roles())
        );

        return Map.of(
                "token", token,
                "userId", matchedUser.userId(),
                "username", matchedUser.username(),
                "roles", matchedUser.roles()
        );
    }

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
