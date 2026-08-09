package com.mssousa.authserver.adapter.out.security.oauth2;

import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.ClientSecret;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemRegisteredClientRepositoryTest {

    @Mock
    private SystemRepository systemRepository;

    private SystemRegisteredClientRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SystemRegisteredClientRepository(systemRepository);
    }

    private System publicSystem() {
        return System.builder()
                .id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM Acme")
                .publicClient(true)
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                .redirectUri(RedirectUri.of("https://crm.acme.com/dev-callback"))
                .build();
    }

    private System confidentialSystem() {
        return System.builder()
                .id(SystemId.of(2L)).clientId(ClientId.of("BACKOFFICE_ACME")).name("Backoffice Acme")
                .publicClient(false).clientSecret(ClientSecret.fromPlainText("super-secreto"))
                .redirectUri(RedirectUri.of("https://backoffice.acme.com/callback"))
                .build();
    }

    private System thirdPartySystem() {
        return System.builder()
                .id(SystemId.of(3L)).clientId(ClientId.of("PARCEIRO_EXTERNO")).name("Parceiro Externo")
                .publicClient(true).thirdParty(true)
                .redirectUri(RedirectUri.of("https://parceiro.example.com/callback"))
                .build();
    }

    @Test
    void deveConverterSistemaPublicoParaRegisteredClientSemSecret() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(publicSystem()));

        RegisteredClient client = repository.findByClientId("CRM_ACME");

        assertNotNull(client);
        assertEquals("1", client.getId());
        assertEquals("CRM_ACME", client.getClientId());
        assertNull(client.getClientSecret());
        assertTrue(client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE));
        assertEquals(2, client.getRedirectUris().size());
        assertTrue(client.getRedirectUris().contains("https://crm.acme.com/callback"));
    }

    @Test
    void deveConverterSistemaConfidencialParaRegisteredClientComSecretHasheado() {
        when(systemRepository.findByClientId(ClientId.of("BACKOFFICE_ACME"))).thenReturn(Optional.of(confidentialSystem()));

        RegisteredClient client = repository.findByClientId("BACKOFFICE_ACME");

        assertNotNull(client);
        assertNotNull(client.getClientSecret());
        assertNotEquals("super-secreto", client.getClientSecret());
        assertTrue(client.getClientSecret().startsWith("{bcrypt}"));
        assertTrue(client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
    }

    @Test
    void deveExigirPkceETtlsCorretosParaTodoClient() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(publicSystem()));

        RegisteredClient client = repository.findByClientId("CRM_ACME");

        assertTrue(client.getClientSettings().isRequireProofKey());
        assertFalse(client.getClientSettings().isRequireAuthorizationConsent());
        assertEquals(Duration.ofMinutes(15), client.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(Duration.ofHours(8), client.getTokenSettings().getRefreshTokenTimeToLive());
        assertFalse(client.getTokenSettings().isReuseRefreshTokens());
    }

    @Test
    void deveExigirConsentimentoParaClientDeTerceiro() {
        when(systemRepository.findByClientId(ClientId.of("PARCEIRO_EXTERNO"))).thenReturn(Optional.of(thirdPartySystem()));

        RegisteredClient client = repository.findByClientId("PARCEIRO_EXTERNO");

        assertTrue(client.getClientSettings().isRequireAuthorizationConsent());
        assertTrue(client.getScopes().contains("profile"));
    }

    @Test
    void deveConcederAuthorizationCodeERefreshToken() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(publicSystem()));

        RegisteredClient client = repository.findByClientId("CRM_ACME");

        assertTrue(client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE));
        assertTrue(client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
    }

    @Test
    void deveRetornarNuloParaClientIdInexistente() {
        when(systemRepository.findByClientId(ClientId.of("DESCONHECIDO"))).thenReturn(Optional.empty());
        assertNull(repository.findByClientId("DESCONHECIDO"));
    }

    @Test
    void deveRetornarNuloParaClientIdEmFormatoInvalido() {
        assertNull(repository.findByClientId("formato inválido com espaço"));
    }

    @Test
    void deveBuscarPorIdNumerico() {
        when(systemRepository.findById(SystemId.of(1L))).thenReturn(Optional.of(publicSystem()));

        RegisteredClient client = repository.findById("1");
        assertNotNull(client);
        assertEquals("CRM_ACME", client.getClientId());
    }

    @Test
    void deveRetornarNuloParaIdNaoNumerico() {
        assertNull(repository.findById("nao-numerico"));
    }

    @Test
    void deveLancarExcecaoAoTentarSalvar() {
        RegisteredClient client = RegisteredClient.withId("x").clientId("x")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://example.com/callback")
                .build();
        assertThrows(UnsupportedOperationException.class, () -> repository.save(client));
    }
}
