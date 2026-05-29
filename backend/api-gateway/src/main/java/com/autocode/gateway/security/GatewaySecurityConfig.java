package com.autocode.gateway.security;

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

    public GatewaySecurityConfig(GatewayAuthenticationEntryPoint authenticationEntryPoint,
                                 DevBearerAuthenticationFilter devBearerAuthenticationFilter,
                                 ProjectPermissionFilter projectPermissionFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.devBearerAuthenticationFilter = devBearerAuthenticationFilter;
        this.projectPermissionFilter = projectPermissionFilter;
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
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(devBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(projectPermissionFilter, DevBearerAuthenticationFilter.class);

        return http.build();
    }
}
