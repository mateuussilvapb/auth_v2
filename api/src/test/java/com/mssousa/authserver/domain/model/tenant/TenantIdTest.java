package com.mssousa.authserver.domain.model.tenant;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantIdTest {

    @Test
    void deveCriarTenantIdValido() {
        TenantId id = TenantId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> TenantId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantId.of(0L));
        assertEquals("TenantId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantId.of(-1L));
        assertEquals("TenantId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(TenantId.of(10L), TenantId.of(10L));
        assertNotEquals(TenantId.of(10L), TenantId.of(20L));
    }

    @Test
    void deveTerHashCodeConsistente() {
        assertEquals(TenantId.of(10L).hashCode(), TenantId.of(10L).hashCode());
    }
}
