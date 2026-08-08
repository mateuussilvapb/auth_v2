package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemIdTest {

    @Test
    void deveCriarSystemIdValido() {
        SystemId id = SystemId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> SystemId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> SystemId.of(0L));
        assertEquals("SystemId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> SystemId.of(-1L));
        assertEquals("SystemId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(SystemId.of(10L), SystemId.of(10L));
        assertNotEquals(SystemId.of(10L), SystemId.of(20L));
    }
}
