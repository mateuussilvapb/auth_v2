package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedirectUriTest {

    @Test
    void deveCriarRedirectUriHttpsValida() {
        RedirectUri uri = RedirectUri.of("https://crm.acme.com/callback");
        assertEquals("https://crm.acme.com/callback", uri.value());
    }

    @Test
    void deveCriarRedirectUriHttpValida() {
        RedirectUri uri = RedirectUri.of("http://localhost:4200/callback");
        assertEquals("http://localhost:4200/callback", uri.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> RedirectUri.of(null));
        assertEquals(RedirectUri.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> RedirectUri.of(""));
        assertEquals(RedirectUri.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaEsquemaInvalido() {
        DomainException exception = assertThrows(DomainException.class, () -> RedirectUri.of("ftp://crm.acme.com"));
        assertEquals(RedirectUri.ERROR_SCHEME, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaUriMuitoLonga() {
        String longUri = "https://acme.com/" + "a".repeat(500);
        DomainException exception = assertThrows(DomainException.class, () -> RedirectUri.of(longUri));
        assertEquals(RedirectUri.ERROR_MAX_LENGTH, exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(RedirectUri.of("https://acme.com/cb"), RedirectUri.of("https://acme.com/cb"));
        assertNotEquals(RedirectUri.of("https://acme.com/cb"), RedirectUri.of("https://acme.com/other"));
    }
}
