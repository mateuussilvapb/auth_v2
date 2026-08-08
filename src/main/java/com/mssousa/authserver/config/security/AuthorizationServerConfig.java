package com.mssousa.authserver.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Estado da autorização (código, tokens em voo) persistido nas tabelas oficiais do
 * Spring Authorization Server (V11 — seção 4.3), para sobreviver a restart. O
 * {@code RegisteredClientRepository} (customizado sobre {@code system}) e o
 * {@code JWKSource} são fornecidos por outras classes deste pacote; issuer, PKCE
 * obrigatório e TTLs por client já estão definidos (application.yml e
 * {@code SystemRegisteredClientRepository}) — o Spring Boot monta o restante da
 * infraestrutura do Authorization Server via autoconfiguração a partir destes beans.
 */
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public OAuth2AuthorizationService oauth2AuthorizationService(JdbcTemplate jdbcTemplate,
                                                                   RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }
}
