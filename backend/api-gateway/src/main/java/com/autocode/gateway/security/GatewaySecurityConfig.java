package com.autocode.gateway.security;

import com.autocode.gateway.audit.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class GatewaySecurityConfig {

    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;
    private final DevBearerAuthenticationFilter devBearerAuthenticationFilter;
    private final ProjectPermissionFilter projectPermissionFilter;
    private final RateLimitFilter rateLimitFilter;

    public GatewaySecurityConfig(GatewayAuthenticationEntryPoint authenticationEntryPoint,
                                 DevBearerAuthenticationFilter devBearerAuthenticationFilter,
                                 ProjectPermissionFilter projectPermissionFilter,
                                 RateLimitFilter rateLimitFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.devBearerAuthenticationFilter = devBearerAuthenticationFilter;
        this.projectPermissionFilter = projectPermissionFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/healthz", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(devBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(projectPermissionFilter, DevBearerAuthenticationFilter.class);

        return http.build();
    }
}
