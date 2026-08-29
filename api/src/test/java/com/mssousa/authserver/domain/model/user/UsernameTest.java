package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsernameTest {

    @Test
    void deveCriarUsernameValido() {
        Username username = Username.of("user123");
        assertEquals("user123", username.value());
    }

    @Test
    void deveCriarUsernameComUnderscore() {
        Username username = Username.of("john_doe");
        assertEquals("john_doe", username.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> Username.of(null));
        assertEquals(Username.DEFAULT_ERROR_USERNAME, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> Username.of(""));
        assertEquals(Username.DEFAULT_ERROR_USERNAME, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorEmBranco() {
        DomainException exception = assertThrows(DomainException.class, () -> Username.of("   "));
        assertEquals(Username.DEFAULT_ERROR_USERNAME, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> Username.of("ab"));
        assertEquals(Username.DEFAULT_ERROR_USERNAME_MIN_LENGTH, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaMuitoLongo() {
        String longUsername = "a".repeat(51);
        DomainException exception = assertThrows(DomainException.class, () -> Username.of(longUsername));
        assertEquals(Username.DEFAULT_ERROR_USERNAME_MAX_LENGTH, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaCaractereEspecial() {
        DomainException exception = assertThrows(DomainException.class, () -> Username.of("user@123"));
        assertEquals(Username.DEFAULT_ERROR_USERNAME_PATTERN, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaEspaco() {
        DomainException exception = assertThrows(DomainException.class, () -> Username.of("user name"));
        assertEquals(Username.DEFAULT_ERROR_USERNAME_PATTERN, exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(Username.of("testuser"), Username.of("testuser"));
        assertNotEquals(Username.of("testuser"), Username.of("outheruser"));
    }

    @Test
    void deveTerToStringIgualAoValor() {
        assertEquals("testuser", Username.of("testuser").toString());
    }
}
