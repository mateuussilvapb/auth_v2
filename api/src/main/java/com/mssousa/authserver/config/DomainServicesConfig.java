package com.mssousa.authserver.config;

import com.mssousa.authserver.domain.service.AccessValidator;
import com.mssousa.authserver.domain.service.PlatformAdminPolicy;
import com.mssousa.authserver.domain.service.ProfileUniquenessPolicy;
import com.mssousa.authserver.domain.service.TenantConsistencyValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os domain services (POJOs puros, sem dependência de framework — regra 5.1.1
 * do plano) como beans Spring, para que a camada de aplicação possa injetá-los. Este é o
 * único lugar em que {@code domain.service.*} é referenciado fora do próprio domínio ou
 * de application — {@code config} pode depender livremente de Spring.
 */
@Configuration
public class DomainServicesConfig {

    @Bean
    public TenantConsistencyValidator tenantConsistencyValidator() {
        return new TenantConsistencyValidator();
    }

    @Bean
    public AccessValidator accessValidator() {
        return new AccessValidator();
    }

    @Bean
    public PlatformAdminPolicy platformAdminPolicy() {
        return new PlatformAdminPolicy();
    }

    @Bean
    public ProfileUniquenessPolicy profileUniquenessPolicy() {
        return new ProfileUniquenessPolicy();
    }
}
