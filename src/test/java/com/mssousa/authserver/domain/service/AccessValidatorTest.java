package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.system.SystemStatus;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.tenant.TenantStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.UserStatus;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessValidatorTest {

    private final AccessValidator validator = new AccessValidator();

    private Tenant activeTenant() {
        return Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build();
    }

    private System activeSystem() {
        return System.builder()
                .id(SystemId.of(1L))
                .clientId(ClientId.of("CRM_ACME"))
                .name("CRM Acme")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                .build();
    }

    private SystemTenant activeSystemTenant() {
        return SystemTenant.builder().id(SystemTenantId.of(1L)).tenantId(TenantId.of(1L)).systemId(SystemId.of(1L)).build();
    }

    private User activeUser() {
        return User.builder()
                .id(UserId.of(1L))
                .tenantId(TenantId.of(1L))
                .username(Username.of("joao_silva"))
                .email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("João da Silva")
                .build();
    }

    private UserSystem activeUserSystem() {
        return UserSystem.builder()
                .id(UserSystemId.of(1L))
                .userId(UserId.of(1L))
                .systemId(SystemId.of(1L))
                .tenantId(TenantId.of(1L))
                .build();
    }

    private UserSystemProfile activeUserSystemProfile() {
        return UserSystemProfile.builder()
                .id(UserSystemProfileId.of(1L))
                .userSystemId(UserSystemId.of(1L))
                .systemProfileId(SystemProfileId.of(1L))
                .build();
    }

    private SystemProfile activeProfile() {
        return SystemProfile.builder()
                .id(SystemProfileId.of(1L))
                .systemId(SystemId.of(1L))
                .code(ProfileCode.of("ADMIN"))
                .build();
    }

    @Test
    void deveValidarLoginQuandoTodosNiveisEstaoAtivos() {
        assertDoesNotThrow(() -> validator.validateLoginAccess(
                activeTenant(), activeSystem(), activeSystemTenant(), activeUser(), activeUserSystem()));
    }

    @Test
    void deveLancarExcecaoQuandoTenantInativo() {
        Tenant tenant = activeTenant();
        tenant.deactivate();

        DomainException exception = assertThrows(DomainException.class, () -> validator.validateLoginAccess(
                tenant, activeSystem(), activeSystemTenant(), activeUser(), activeUserSystem()));
        assertEquals(AccessValidator.ERROR_TENANT_INACTIVE, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSistemaInativo() {
        System system = activeSystem();
        system.deactivate();

        DomainException exception = assertThrows(DomainException.class, () -> validator.validateLoginAccess(
                activeTenant(), system, activeSystemTenant(), activeUser(), activeUserSystem()));
        assertEquals(AccessValidator.ERROR_SYSTEM_INACTIVE, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoVinculoSistemaTenantInativo() {
        SystemTenant systemTenant = activeSystemTenant();
        systemTenant.deactivate();

        DomainException exception = assertThrows(DomainException.class, () -> validator.validateLoginAccess(
                activeTenant(), activeSystem(), systemTenant, activeUser(), activeUserSystem()));
        assertEquals(SystemTenant.ERROR_INACTIVE_BINDING, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioInativo() {
        User user = activeUser();
        user.block();

        DomainException exception = assertThrows(DomainException.class, () -> validator.validateLoginAccess(
                activeTenant(), activeSystem(), activeSystemTenant(), user, activeUserSystem()));
        assertEquals(AccessValidator.ERROR_USER_INACTIVE, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoVinculoUsuarioSistemaInativo() {
        UserSystem userSystem = activeUserSystem();
        userSystem.block();

        DomainException exception = assertThrows(DomainException.class, () -> validator.validateLoginAccess(
                activeTenant(), activeSystem(), activeSystemTenant(), activeUser(), userSystem));
        assertEquals(UserSystem.ERROR_INACTIVE_BINDING, exception.getMessage());
    }

    @Test
    void canLoginRetornaFalsoSemLancarExcecao() {
        Tenant tenant = activeTenant();
        tenant.deactivate();

        assertFalse(validator.canLogin(tenant, activeSystem(), activeSystemTenant(), activeUser(), activeUserSystem()));
    }

    @Test
    void canLoginRetornaVerdadeiroQuandoTudoAtivo() {
        assertTrue(validator.canLogin(activeTenant(), activeSystem(), activeSystemTenant(), activeUser(), activeUserSystem()));
    }

    @Test
    void deveValidarPerfilQuandoVinculoEPerfilAtivos() {
        assertDoesNotThrow(() -> validator.validateProfileAccess(activeUserSystemProfile(), activeProfile()));
    }

    @Test
    void deveLancarExcecaoQuandoVinculoUsuarioPerfilInativo() {
        UserSystemProfile binding = activeUserSystemProfile();
        binding.deactivate();

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validateProfileAccess(binding, activeProfile()));
        assertEquals(UserSystemProfile.ERROR_INACTIVE_BINDING, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoPerfilInativo() {
        SystemProfile profile = activeProfile();
        profile.deactivate();

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validateProfileAccess(activeUserSystemProfile(), profile));
        assertEquals(AccessValidator.ERROR_PROFILE_INACTIVE, exception.getMessage());
    }
}
