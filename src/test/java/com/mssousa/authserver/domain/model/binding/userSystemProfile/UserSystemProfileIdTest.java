package com.mssousa.authserver.domain.model.binding.userSystemProfile;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSystemProfileIdTest {

    @Test
    void deveCriarUserSystemProfileIdValido() {
        UserSystemProfileId id = UserSystemProfileId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> UserSystemProfileId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> UserSystemProfileId.of(0L));
        assertEquals("UserSystemProfileId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> UserSystemProfileId.of(-1L));
        assertEquals("UserSystemProfileId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(UserSystemProfileId.of(10L), UserSystemProfileId.of(10L));
        assertNotEquals(UserSystemProfileId.of(10L), UserSystemProfileId.of(20L));
    }
}
