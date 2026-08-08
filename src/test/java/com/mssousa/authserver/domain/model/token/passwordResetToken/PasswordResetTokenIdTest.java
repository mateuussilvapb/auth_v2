package com.mssousa.authserver.domain.model.token.passwordResetToken;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenIdTest {

    @Test
    void deveCriarIdValido() {
        PasswordResetTokenId id = PasswordResetTokenId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> PasswordResetTokenId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> PasswordResetTokenId.of(0L));
        assertEquals("PasswordResetTokenId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> PasswordResetTokenId.of(-1L));
        assertEquals("PasswordResetTokenId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(PasswordResetTokenId.of(10L), PasswordResetTokenId.of(10L));
        assertNotEquals(PasswordResetTokenId.of(10L), PasswordResetTokenId.of(20L));
    }
}
