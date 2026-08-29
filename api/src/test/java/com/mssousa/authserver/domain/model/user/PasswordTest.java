package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    @Test
    void deveCriarPasswordAPartirDeTextoPlano() {
        Password password = Password.fromPlainText("senhaSegura123");
        assertTrue(password.matches("senhaSegura123"));
    }

    @Test
    void hashNaoDeveSerIgualAoTextoPlano() {
        Password password = Password.fromPlainText("senhaSegura123");
        assertNotEquals("senhaSegura123", password.hashedValue());
    }

    @Test
    void naoDeveCorresponderASenhaErrada() {
        Password password = Password.fromPlainText("senhaSegura123");
        assertFalse(password.matches("outraSenha"));
    }

    @Test
    void naoDeveCorresponderASenhaNula() {
        Password password = Password.fromPlainText("senhaSegura123");
        assertFalse(password.matches(null));
    }

    @Test
    void deveLancarExcecaoParaTextoPlanoNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> Password.fromPlainText(null));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaTextoPlanoVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> Password.fromPlainText(""));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaTextoPlanoMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> Password.fromPlainText("1234567"));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD_MIN_LENGTH, exception.getMessage());
    }

    @Test
    void deveReconstruirAPartirDeHash() {
        Password original = Password.fromPlainText("senhaSegura123");
        Password reconstruida = Password.fromHash(original.hashedValue());
        assertTrue(reconstruida.matches("senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoParaHashNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> Password.fromHash(null));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaHashVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> Password.fromHash(""));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void naoDeveExporToStringComSenha() {
        Password password = Password.fromPlainText("senhaSegura123");
        assertFalse(password.toString().contains("senhaSegura123"));
    }
}
