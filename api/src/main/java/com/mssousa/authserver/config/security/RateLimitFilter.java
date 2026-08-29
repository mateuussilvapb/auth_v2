package com.mssousa.authserver.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>
 * Limites configuráveis via {@code authserver.rate-limit.*} (application.yml) — o perfil
 * {@code dev} (sob o qual a suíte de testes de integração roda) usa limites bem mais
 * folgados, porque o bucket é um bean singleton compartilhado por todo o contexto Spring:
 * sem isso, testes de integração que fazem várias chamadas a {@code /api/auth/login} numa
 * mesma suíte esbarram no limite pensado para produção.
 * </p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int loginPerMinute;
    private final int tokenPerMinute;
    private final int adminApiPerMinute;

    public RateLimitFilter(
            @Value("${authserver.rate-limit.login:10}") int loginPerMinute,
            @Value("${authserver.rate-limit.token:30}") int tokenPerMinute,
            @Value("${authserver.rate-limit.admin-api:60}") int adminApiPerMinute) {
        this.loginPerMinute = loginPerMinute;
        this.tokenPerMinute = tokenPerMinute;
        this.adminApiPerMinute = adminApiPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = ruleFor(request.getRequestURI());

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.name() + ":" + clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(requestsPerMinute(rule)));

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

    private int requestsPerMinute(RateLimitRule rule) {
        return switch (rule) {
            case LOGIN -> loginPerMinute;
            case TOKEN -> tokenPerMinute;
            case ADMIN_API -> adminApiPerMinute;
        };
    }

    private Bucket newBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
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
        LOGIN, TOKEN, ADMIN_API
    }
}
