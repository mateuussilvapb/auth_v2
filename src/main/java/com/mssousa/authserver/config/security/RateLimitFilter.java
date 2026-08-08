package com.mssousa.authserver.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita a taxa de requisições em {@code /api/auth/login}, {@code /oauth2/token} e
 * {@code /admin/api/**} (seção 7.4 do plano). Um bucket por combinação rota+IP, em
 * memória — suficiente para a instância única do deploy planejado (seção 11); se a
 * arquitetura crescer para múltiplas instâncias, precisa migrar para um backend
 * distribuído (Redis, por exemplo — Bucket4j suporta isso nativamente).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = ruleFor(request.getRequestURI());

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.name() + ":" + clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(rule));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Muitas requisições. Tente novamente em instantes.");
        }
    }

    private RateLimitRule ruleFor(String path) {
        if ("/api/auth/login".equals(path)) {
            return RateLimitRule.LOGIN;
        }
        if ("/oauth2/token".equals(path)) {
            return RateLimitRule.TOKEN;
        }
        if (path.startsWith("/admin/api/")) {
            return RateLimitRule.ADMIN_API;
        }
        return null;
    }

    private Bucket newBucket(RateLimitRule rule) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(rule.requestsPerMinute, Refill.greedy(rule.requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private enum RateLimitRule {
        LOGIN(10),
        TOKEN(30),
        ADMIN_API(60);

        private final int requestsPerMinute;

        RateLimitRule(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
