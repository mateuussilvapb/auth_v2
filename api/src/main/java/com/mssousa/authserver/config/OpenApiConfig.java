package com.mssousa.authserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação da API administrativa (seção 9/Fase 8 do plano) via springdoc-openapi.
 * Cobre só {@code /admin/api/v1/**} — os endpoints públicos de {@code /api/auth/**} são
 * consumidos pelo SPA Angular, não por integradores externos, e não precisam de OpenAPI.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "platformAdminBearer";

    @Bean
    public OpenAPI authServerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Server V2 — API administrativa")
                        .version("v1")
                        .description("API administrativa (seção 9 do plano) — todos os endpoints exigem "
                                + "token JWT de platform admin (claim platform_admin=true)."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
