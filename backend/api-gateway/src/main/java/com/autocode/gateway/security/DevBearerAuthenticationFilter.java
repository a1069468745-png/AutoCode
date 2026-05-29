package com.autocode.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class DevBearerAuthenticationFilter extends OncePerRequestFilter {

    private final String devToken;
    private final TokenService tokenService;

    public DevBearerAuthenticationFilter(@Value("${autocode.gateway.dev-token:dev-token}") String devToken,
                                         TokenService tokenService) {
        this.devToken = devToken;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        // Try dev-token first (development shortcut)
        if (devToken.equals(token)) {
            DevPrincipal principal = new DevPrincipal("dev-user", "developer", List.of("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }

        // Try JWT token
        TokenService.TokenPayload payload = tokenService.validateToken(token);
        if (payload != null) {
            List<String> roleList = Arrays.asList(payload.roles().split(","));
            DevPrincipal principal = new DevPrincipal(payload.userId(), payload.username(), roleList);
            List<SimpleGrantedAuthority> authorities = roleList.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
