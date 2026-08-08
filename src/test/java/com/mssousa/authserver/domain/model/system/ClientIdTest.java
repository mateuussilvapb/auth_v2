package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientIdTest {

    @Test
    void deveCriarClientIdValido() {
        ClientId clientId = ClientId.of("CRM_ACME");
        assertEquals("CRM_ACME", clientId.value());
    }

    @Test
    void deveCriarClientIdComHifen() {
        ClientId clientId = ClientId.of("CRM-ACME-01");
        assertEquals("CRM-ACME-01", clientId.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientId.of(null));
        assertEquals(ClientId.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientId.of(""));
        assertEquals(ClientId.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaLetrasMinusculas() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientId.of("crm_acme"));
        assertEquals(ClientId.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientId.of("AB"));
        assertEquals(ClientId.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaCaractereInvalido() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientId.of("CRM ACME"));
        assertEquals(ClientId.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(ClientId.of("CRM_ACME"), ClientId.of("CRM_ACME"));
        assertNotEquals(ClientId.of("CRM_ACME"), ClientId.of("CRM_GLOBEX"));
    }
}
