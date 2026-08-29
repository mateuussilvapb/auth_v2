package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantConsistencyValidatorTest {

    private final TenantConsistencyValidator validator = new TenantConsistencyValidator();

    private User userOfTenant(long tenantId) {
        return User.builder()
                .id(UserId.of(1L))
                .tenantId(TenantId.of(tenantId))
                .username(Username.of("joao_silva"))
                .email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("João da Silva")
                .build();
    }

    private SystemTenant systemTenantOfTenant(long tenantId) {
        return SystemTenant.builder()
                .id(SystemTenantId.of(1L))
                .tenantId(TenantId.of(tenantId))
                .systemId(SystemId.of(1L))
                .build();
    }

    @Test
    void naoDeveLancarExcecaoQuandoUsuarioESistemaPertencemAoMesmoTenant() {
        User user = userOfTenant(1L);
        SystemTenant systemTenant = systemTenantOfTenant(1L);

        assertDoesNotThrow(() -> validator.validateSameTenant(user, systemTenant));
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioESistemaPertencemATenantsDiferentes() {
        User user = userOfTenant(1L);
        SystemTenant systemTenant = systemTenantOfTenant(2L);

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validateSameTenant(user, systemTenant));
        assertEquals(TenantConsistencyValidator.ERROR_TENANT_MISMATCH, exception.getMessage());
    }
}
