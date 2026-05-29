package com.autocode.gateway.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        RateLimitProperties.RateLimitRule rule = resolveRule(path);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        String bucketKey = path + ":" + clientKey;
        TokenBucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> new TokenBucket(rule.permitsPerSecond(), rule.burstSize()));

        if (!bucket.tryAcquire()) {
            log.warn("Rate limit exceeded path={} clientKey={}", path, clientKey);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "1");
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", "RATE_LIMITED",
                    "message", "Too many requests, please retry later",
                    "timestamp", Instant.now().toString()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitProperties.RateLimitRule resolveRule(String path) {
        if (properties.rules() == null) {
            return null;
        }
        for (Map.Entry<String, RateLimitProperties.RateLimitRule> entry : properties.rules().entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class TokenBucket {
        private final double permitsPerSecond;
        private final int burstSize;
        private final AtomicLong lastRefillNanos;
        private final AtomicInteger tokens;

        TokenBucket(int permitsPerSecond, int burstSize) {
            this.permitsPerSecond = permitsPerSecond;
            this.burstSize = burstSize;
            this.lastRefillNanos = new AtomicLong(System.nanoTime());
            this.tokens = new AtomicInteger(burstSize);
        }

        boolean tryAcquire() {
            refill();
            int current = tokens.get();
            while (current > 0) {
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = tokens.get();
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long last = lastRefillNanos.get();
            long elapsedNs = now - last;
            if (elapsedNs < 100_000_000L) {
                return;
            }
            if (!lastRefillNanos.compareAndSet(last, now)) {
                return;
            }
            double elapsedSeconds = elapsedNs / 1_000_000_000.0;
            int newTokens = (int) (elapsedSeconds * permitsPerSecond);
            if (newTokens > 0) {
                tokens.updateAndGet(current -> Math.min(current + newTokens, burstSize));
            }
        }
    }
}
