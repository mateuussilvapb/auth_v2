package com.mssousa.authserver.domain.model.platform;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformAdminIdTest {

    @Test
    void deveCriarPlatformAdminIdValido() {
        PlatformAdminId id = PlatformAdminId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> PlatformAdminId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> PlatformAdminId.of(0L));
        assertEquals("PlatformAdminId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> PlatformAdminId.of(-1L));
        assertEquals("PlatformAdminId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(PlatformAdminId.of(10L), PlatformAdminId.of(10L));
        assertNotEquals(PlatformAdminId.of(10L), PlatformAdminId.of(20L));
    }
}
