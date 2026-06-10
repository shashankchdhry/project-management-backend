package org.example.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Redis fixed-window rate limiter, keyed by user (per-IP for anonymous). Cross-instance because the
 * counter lives in Redis. Fails open if Redis is unavailable. Registered
 * in the security chain after {@link JwtAuthFilter} so the user is known.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final int limitPerMinute;

    public RateLimitFilter(StringRedisTemplate redis, boolean enabled, int limitPerMinute) {
        this.redis = redis;
        this.enabled = enabled;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled || !request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        String subject = CurrentUser.id().map(UUID::toString).orElse("anon:" + request.getRemoteAddr());
        String key = "rl:" + subject + ":" + (Instant.now().getEpochSecond() / 60);

        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(60));
            }
        } catch (Exception e) {
            chain.doFilter(request, response); // fail open if Redis is down
            return;
        }

        if (count != null && count > limitPerMinute) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"title\":\"Rate limit exceeded\",\"status\":429}");
            return;
        }
        chain.doFilter(request, response);
    }
}
