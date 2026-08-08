package com.mssousa.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Habilita o preenchimento automático de {@code created_at}/{@code created_by}/
 * {@code updated_at} via Spring Data JPA Auditing (seção 5 do plano).
 * <p>
 * Antes da Fase 6 (segurança), não há autenticação configurada — {@code createdBy} cai
 * no valor "system" nesse caso, o que é esperado para seeds/migrações/testes.
 * </p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    private static final String SYSTEM_AUDITOR = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of(SYSTEM_AUDITOR));
    }
}
