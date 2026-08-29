package com.mssousa.authserver.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS para o SPA Angular (seção 7.4/Fase 8 do plano). Em produção o Angular é servido
 * pelo mesmo nginx/domínio do auth server (seção 11) — CORS só é necessário em
 * desenvolvimento local, onde {@code ng serve} roda numa origem diferente
 * ({@code http://localhost:4200}). {@code authserver.cors.allowed-origins} vazio (default
 * de produção) significa nenhuma origem cross-origin liberada.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${authserver.cors.allowed-origins:}") String allowedOrigins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.isEmpty()) {
            return source;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        source.registerCorsConfiguration("/api/auth/**", configuration);
        source.registerCorsConfiguration("/oauth2/**", configuration);
        source.registerCorsConfiguration("/admin/api/**", configuration);
        // Documento de descoberta OIDC (/.well-known/openid-configuration), consumido pelo
        // OAuthService (angular-oauth2-oidc) do console administrativo antes de disparar
        // initCodeFlow — sem CORS aqui, o fetch cross-origin falha em dev (ng serve em
        // localhost:4200, backend em localhost:8080; mesma origem em produção, seção 11).
        source.registerCorsConfiguration("/.well-known/**", configuration);
        return source;
    }
}
