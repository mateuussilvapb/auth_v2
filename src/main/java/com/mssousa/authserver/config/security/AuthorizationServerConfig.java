package com.mssousa.authserver.config.security;

import com.mssousa.authserver.config.security.jackson.OAuth2AuthorizationJsonMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Estado da autorização (código, tokens em voo) persistido nas tabelas oficiais do
 * Spring Authorization Server (V11 — seção 4.3), para sobreviver a restart. O
 * {@code RegisteredClientRepository} (customizado sobre {@code system}) e o
 * {@code JWKSource} são fornecidos por outras classes deste pacote; issuer, PKCE
 * obrigatório e TTLs por client já estão definidos (application.yml e
 * {@code SystemRegisteredClientRepository}) — o Spring Boot monta o restante da
 * infraestrutura do Authorization Server via autoconfiguração a partir destes beans.
 * <p>
 * O row mapper e o parameters mapper são trocados pela variante {@code JsonMapper}
 * (Jackson 3) de {@link OAuth2AuthorizationJsonMapperFactory}, que conhece
 * {@code ClientAwareAuthenticationToken} e {@code AuthenticatedUser} — sem isso, reler uma
 * authorization persistida (troca de código por token) falha na desserialização do
 * principal do resource owner. Construído diretamente (não via {@code @Bean}) para não
 * ser adotado pela autoconfiguração do Boot como o {@code JsonMapper} global do Spring
 * MVC — ver javadoc da factory.
 * </p>
 */
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public OAuth2AuthorizationService oauth2AuthorizationService(JdbcTemplate jdbcTemplate,
                                                                   RegisteredClientRepository registeredClientRepository) {
        JsonMapper oauth2AuthorizationJsonMapper = OAuth2AuthorizationJsonMapperFactory.build();

        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        service.setAuthorizationRowMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                        registeredClientRepository, oauth2AuthorizationJsonMapper));
        service.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(
                        oauth2AuthorizationJsonMapper));
        return service;
    }
}
