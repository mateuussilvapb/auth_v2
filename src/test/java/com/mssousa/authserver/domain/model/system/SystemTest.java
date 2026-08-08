package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemTest {

    private System.Builder publicClientBuilder() {
        return System.builder()
                .id(SystemId.of(1L))
                .clientId(ClientId.of("CRM_ACME"))
                .name("CRM Acme")
                .publicClient(true)
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback"));
    }

    private System.Builder confidentialClientBuilder() {
        return System.builder()
                .id(SystemId.of(2L))
                .clientId(ClientId.of("BACKOFFICE_ACME"))
                .name("Backoffice Acme")
                .publicClient(false)
                .clientSecret("super-secret")
                .redirectUri(RedirectUri.of("https://backoffice.acme.com/callback"));
    }

    @Test
    void deveCriarClientPublicoValidoSemSecret() {
        System system = publicClientBuilder().build();

        assertEquals(SystemId.of(1L), system.getId());
        assertEquals(ClientId.of("CRM_ACME"), system.getClientId());
        assertTrue(system.isPublicClient());
        assertNull(system.getClientSecret());
        assertEquals(1, system.getRedirectUris().size());
        assertTrue(system.isActive());
    }

    @Test
    void deveCriarClientConfidencialValidoComSecret() {
        System system = confidentialClientBuilder().build();

        assertFalse(system.isPublicClient());
        assertEquals("super-secret", system.getClientSecret());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> System.builder()
                        .clientId(ClientId.of("CRM_ACME"))
                        .name("CRM")
                        .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                        .build());
        assertEquals(System.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoClientIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> System.builder()
                        .id(SystemId.of(1L))
                        .name("CRM")
                        .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                        .build());
        assertEquals(ClientId.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNomeNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> System.builder()
                        .id(SystemId.of(1L))
                        .clientId(ClientId.of("CRM_ACME"))
                        .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                        .build());
        assertEquals(System.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSemRedirectUri() {
        DomainException exception = assertThrows(DomainException.class,
                () -> System.builder()
                        .id(SystemId.of(1L))
                        .clientId(ClientId.of("CRM_ACME"))
                        .name("CRM")
                        .build());
        assertEquals(System.ERROR_REDIRECT_URI_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoConfidencialSemSecret() {
        DomainException exception = assertThrows(DomainException.class,
                () -> System.builder()
                        .id(SystemId.of(1L))
                        .clientId(ClientId.of("CRM_ACME"))
                        .name("CRM")
                        .publicClient(false)
                        .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                        .build());
        assertEquals(System.ERROR_SECRET_REQUIRED_FOR_CONFIDENTIAL, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoPublicoComSecret() {
        DomainException exception = assertThrows(DomainException.class,
                () -> System.builder()
                        .id(SystemId.of(1L))
                        .clientId(ClientId.of("CRM_ACME"))
                        .name("CRM")
                        .publicClient(true)
                        .clientSecret("nao-deveria-existir")
                        .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                        .build());
        assertEquals(System.ERROR_SECRET_NOT_ALLOWED_FOR_PUBLIC, exception.getMessage());
    }

    @Test
    void deveAtivarEDesativarSistema() {
        System system = publicClientBuilder().build();
        system.deactivate();
        assertFalse(system.isActive());
        assertFalse(system.canAcceptAuthentication());

        system.activate();
        assertTrue(system.isActive());
        assertTrue(system.canAcceptAuthentication());
    }

    @Test
    void deveAdicionarRedirectUri() {
        System system = publicClientBuilder().build();
        system.addRedirectUri(RedirectUri.of("https://crm.acme.com/dev-callback"));
        assertEquals(2, system.getRedirectUris().size());
    }

    @Test
    void deveRemoverRedirectUriQuandoHaMaisDeUma() {
        System system = publicClientBuilder().build();
        RedirectUri segunda = RedirectUri.of("https://crm.acme.com/dev-callback");
        system.addRedirectUri(segunda);

        system.removeRedirectUri(segunda);

        assertEquals(1, system.getRedirectUris().size());
    }

    @Test
    void deveLancarExcecaoAoRemoverUltimaRedirectUri() {
        System system = publicClientBuilder().build();
        RedirectUri unica = system.getRedirectUris().get(0);

        DomainException exception = assertThrows(DomainException.class, () -> system.removeRedirectUri(unica));
        assertEquals(System.ERROR_LAST_REDIRECT_URI, exception.getMessage());
    }

    @Test
    void deveConfirmarRedirectUriRegistrada() {
        System system = publicClientBuilder().build();
        assertTrue(system.matchesRedirectUri("https://crm.acme.com/callback"));
        assertFalse(system.matchesRedirectUri("https://outro.com/callback"));
    }

    @Test
    void deveRotacionarSecretDeClientConfidencial() {
        System system = confidentialClientBuilder().build();
        system.rotateSecret("novo-secret");
        assertEquals("novo-secret", system.getClientSecret());
        assertTrue(system.verifyClientSecret("novo-secret"));
        assertFalse(system.verifyClientSecret("super-secret"));
    }

    @Test
    void deveLancarExcecaoAoRotacionarSecretDeClientPublico() {
        System system = publicClientBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> system.rotateSecret("qualquer"));
        assertEquals(System.ERROR_SECRET_NOT_ALLOWED_FOR_PUBLIC, exception.getMessage());
    }

    @Test
    void clientPublicoNuncaValidaSecret() {
        System system = publicClientBuilder().build();
        assertFalse(system.verifyClientSecret("qualquer-coisa"));
    }
}
