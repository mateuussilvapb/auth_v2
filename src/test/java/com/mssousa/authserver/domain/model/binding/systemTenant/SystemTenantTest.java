package com.mssousa.authserver.domain.model.binding.systemTenant;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemTenantTest {

    private SystemTenant.Builder validBuilder() {
        return SystemTenant.builder()
                .id(SystemTenantId.of(1L))
                .tenantId(TenantId.of(1L))
                .systemId(SystemId.of(1L));
    }

    @Test
    void deveCriarVinculoValidoComStatusPadraoActive() {
        SystemTenant binding = validBuilder().build();

        assertEquals(SystemTenantId.of(1L), binding.getId());
        assertEquals(TenantId.of(1L), binding.getTenantId());
        assertEquals(SystemId.of(1L), binding.getSystemId());
        assertTrue(binding.isActive());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> SystemTenant.builder().tenantId(TenantId.of(1L)).systemId(SystemId.of(1L)).build());
        assertEquals(SystemTenant.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoTenantIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> SystemTenant.builder().id(SystemTenantId.of(1L)).systemId(SystemId.of(1L)).build());
        assertEquals(SystemTenant.ERROR_TENANT_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSystemIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> SystemTenant.builder().id(SystemTenantId.of(1L)).tenantId(TenantId.of(1L)).build());
        assertEquals(SystemTenant.ERROR_SYSTEM_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtivarDesativarEBloquear() {
        SystemTenant binding = validBuilder().build();

        binding.deactivate();
        assertTrue(binding.isInactive());

        binding.block();
        assertTrue(binding.isBlocked());

        binding.activate();
        assertTrue(binding.isActive());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        SystemTenant binding = validBuilder().status(BindingStatus.INACTIVE).build();
        binding.activate();
        binding.activate();
        assertTrue(binding.isActive());
    }

    @Test
    void validateAccessNaoLancaExcecaoQuandoAtivo() {
        SystemTenant binding = validBuilder().build();
        assertDoesNotThrow(binding::validateAccess);
    }

    @Test
    void validateAccessLancaExcecaoQuandoInativo() {
        SystemTenant binding = validBuilder().status(BindingStatus.INACTIVE).build();
        DomainException exception = assertThrows(DomainException.class, binding::validateAccess);
        assertEquals(SystemTenant.ERROR_INACTIVE_BINDING, exception.getMessage());
    }

    @Test
    void validateAccessLancaExcecaoQuandoBloqueado() {
        SystemTenant binding = validBuilder().status(BindingStatus.BLOCKED).build();
        DomainException exception = assertThrows(DomainException.class, binding::validateAccess);
        assertEquals(SystemTenant.ERROR_INACTIVE_BINDING, exception.getMessage());
    }
}
