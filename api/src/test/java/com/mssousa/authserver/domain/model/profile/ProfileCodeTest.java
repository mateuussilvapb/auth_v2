package com.mssousa.authserver.domain.model.profile;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCodeTest {

    @Test
    void deveCriarCodigoValido() {
        ProfileCode code = ProfileCode.of("ADMIN");
        assertEquals("ADMIN", code.value());
    }

    @Test
    void deveNormalizarParaMaiusculasETrim() {
        ProfileCode code = ProfileCode.of("  financeiro  ");
        assertEquals("FINANCEIRO", code.value());
    }

    @Test
    void deveCriarCodigoComUnderscore() {
        ProfileCode code = ProfileCode.of("OPERADOR_CAIXA");
        assertEquals("OPERADOR_CAIXA", code.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> ProfileCode.of(null));
        assertEquals(ProfileCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> ProfileCode.of(""));
        assertEquals(ProfileCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorEmBranco() {
        DomainException exception = assertThrows(DomainException.class, () -> ProfileCode.of("   "));
        assertEquals(ProfileCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> ProfileCode.of("A"));
        assertEquals(ProfileCode.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaCaractereInvalido() {
        DomainException exception = assertThrows(DomainException.class, () -> ProfileCode.of("ADMIN-01"));
        assertEquals(ProfileCode.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveSerIgualPorValorNormalizado() {
        assertEquals(ProfileCode.of("admin"), ProfileCode.of("ADMIN"));
        assertNotEquals(ProfileCode.of("ADMIN"), ProfileCode.of("FINANCEIRO"));
    }
}
