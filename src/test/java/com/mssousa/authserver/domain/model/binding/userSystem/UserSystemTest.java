package com.mssousa.authserver.domain.model.binding.userSystem;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSystemTest {

    private UserSystem.Builder validBuilder() {
        return UserSystem.builder()
                .id(UserSystemId.of(1L))
                .userId(UserId.of(1L))
                .systemId(SystemId.of(1L))
                .tenantId(TenantId.of(1L));
    }

    @Test
    void deveCriarVinculoValidoComStatusPadraoActive() {
        UserSystem binding = validBuilder().build();

        assertEquals(UserSystemId.of(1L), binding.getId());
        assertEquals(UserId.of(1L), binding.getUserId());
        assertEquals(SystemId.of(1L), binding.getSystemId());
        assertEquals(TenantId.of(1L), binding.getTenantId());
        assertTrue(binding.isActive());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystem.builder().userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build());
        assertEquals(UserSystem.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoUserIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystem.builder().id(UserSystemId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build());
        assertEquals(UserSystem.ERROR_USER_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSystemIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystem.builder().id(UserSystemId.of(1L)).userId(UserId.of(1L)).tenantId(TenantId.of(1L)).build());
        assertEquals(UserSystem.ERROR_SYSTEM_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoTenantIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystem.builder().id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).build());
        assertEquals(UserSystem.ERROR_TENANT_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtivarDesativarEBloquear() {
        UserSystem binding = validBuilder().build();

        binding.deactivate();
        assertTrue(binding.isInactive());

        binding.block();
        assertTrue(binding.isBlocked());

        binding.activate();
        assertTrue(binding.isActive());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        UserSystem binding = validBuilder().status(BindingStatus.BLOCKED).build();
        binding.activate();
        binding.activate();
        assertTrue(binding.isActive());
    }

    @Test
    void validateAccessNaoLancaExcecaoQuandoAtivo() {
        UserSystem binding = validBuilder().build();
        assertDoesNotThrow(binding::validateAccess);
    }

    @Test
    void validateAccessLancaExcecaoQuandoBloqueado() {
        UserSystem binding = validBuilder().status(BindingStatus.BLOCKED).build();
        DomainException exception = assertThrows(DomainException.class, binding::validateAccess);
        assertEquals(UserSystem.ERROR_INACTIVE_BINDING, exception.getMessage());
    }
}
