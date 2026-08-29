package com.mssousa.authserver.domain.model.binding.systemTenant;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemTenantIdTest {

    @Test
    void deveCriarSystemTenantIdValido() {
        SystemTenantId id = SystemTenantId.of(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(NullPointerException.class, () -> SystemTenantId.of(null));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        DomainException exception = assertThrows(DomainException.class, () -> SystemTenantId.of(0L));
        assertEquals("SystemTenantId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        DomainException exception = assertThrows(DomainException.class, () -> SystemTenantId.of(-1L));
        assertEquals("SystemTenantId deve ser um número positivo", exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(SystemTenantId.of(10L), SystemTenantId.of(10L));
        assertNotEquals(SystemTenantId.of(10L), SystemTenantId.of(20L));
    }
}
