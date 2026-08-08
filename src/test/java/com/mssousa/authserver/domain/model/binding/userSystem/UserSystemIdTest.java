package com.mssousa.authserver.domain.model.binding.userSystem;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSystemIdTest {

    @Test
    void deveCriarUserSystemIdValido() {
        UserSystemId id = UserSystemId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> UserSystemId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> UserSystemId.of(0L));
        assertEquals("UserSystemId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> UserSystemId.of(-1L));
        assertEquals("UserSystemId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(UserSystemId.of(10L), UserSystemId.of(10L));
        assertNotEquals(UserSystemId.of(10L), UserSystemId.of(20L));
    }
}
