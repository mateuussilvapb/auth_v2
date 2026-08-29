package com.mssousa.authserver.domain.model.profile;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemProfileIdTest {

    @Test
    void deveCriarSystemProfileIdValido() {
        SystemProfileId id = SystemProfileId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> SystemProfileId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> SystemProfileId.of(0L));
        assertEquals("SystemProfileId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> SystemProfileId.of(-1L));
        assertEquals("SystemProfileId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(SystemProfileId.of(10L), SystemProfileId.of(10L));
        assertNotEquals(SystemProfileId.of(10L), SystemProfileId.of(20L));
    }
}
