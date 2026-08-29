package com.mssousa.authserver.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Confirma que o schema V11 (oauth2_authorization, seção 4.3) bate exatamente com o que
 * JdbcOAuth2AuthorizationService espera — sem isso, um simples save()/find() já falha.
 */
class JdbcOAuth2AuthorizationServiceIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Test
    void deveSerUmaInstanciaDeJdbcOAuth2AuthorizationService() {
        assertInstanceOf(JdbcOAuth2AuthorizationService.class, authorizationService);
    }

    @Test
    void deveSalvarEBuscarAutorizacaoPorId() {
        com.mssousa.authserver.domain.model.system.System system = createAndSaveSystem("CRM_TESTE");

        RegisteredClient registeredClient = RegisteredClient.withId(system.getId().value().toString())
                .clientId(system.getClientId().value())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://crm_teste.example.com/callback")
                .build();

        String authorizationId = UUID.randomUUID().toString();
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(authorizationId)
                .principalName("joao_silva")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-de-teste",
                        Instant.now(), Instant.now().plusSeconds(900)))
                .build();

        authorizationService.save(authorization);

        OAuth2Authorization found = authorizationService.findById(authorizationId);
        assertNotNull(found);
        assertEquals("joao_silva", found.getPrincipalName());
        assertEquals("token-de-teste", found.getAccessToken().getToken().getTokenValue());
    }
}
