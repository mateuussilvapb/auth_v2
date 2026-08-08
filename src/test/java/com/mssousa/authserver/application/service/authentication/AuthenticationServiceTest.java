package com.mssousa.authserver.application.service.authentication;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import com.mssousa.authserver.domain.service.AccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemTenantRepository systemTenantRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSystemRepository userSystemRepository;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(systemRepository, systemTenantRepository, tenantRepository,
                userRepository, userSystemRepository, new AccessValidator());
    }

    private Tenant activeTenant() {
        return Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build();
    }

    private System activeSystem() {
        return System.builder().id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build();
    }

    private SystemTenant activeSystemTenant() {
        return SystemTenant.builder().id(SystemTenantId.of(1L)).tenantId(TenantId.of(1L)).systemId(SystemId.of(1L)).build();
    }

    private User activeUser() {
        return User.builder().id(UserId.of(1L)).tenantId(TenantId.of(1L))
                .username(Username.of("joao_silva")).email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123")).name("João da Silva").build();
    }

    private UserSystem activeUserSystem() {
        return UserSystem.builder().id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L))
                .tenantId(TenantId.of(1L)).build();
    }

    private void stubHappyPathUpTo(User user) {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(activeTenant()));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(Optional.of(activeUserSystem()));
    }

    @Test
    void deveAutenticarPorUsernameQuandoTudoAtivo() {
        stubHappyPathUpTo(activeUser());
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(activeUser()));

        AuthenticatedUser result = service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123");

        assertEquals(UserId.of(1L), result.userId());
        assertEquals(TenantId.of(1L), result.tenantId());
        assertEquals(SystemId.of(1L), result.systemId());
    }

    @Test
    void deveAutenticarPorEmailQuandoUsernameNaoResolve() {
        stubHappyPathUpTo(activeUser());
        when(userRepository.findByTenantIdAndEmail(TenantId.of(1L), Email.of("joao@acme.com")))
                .thenReturn(Optional.of(activeUser()));

        AuthenticatedUser result = service.authenticate("CRM_ACME", "joao@acme.com", "senhaSegura123");
        assertEquals(UserId.of(1L), result.userId());
    }

    @Test
    void deveLancarExcecaoGenericaParaClientIdInexistente() {
        when(systemRepository.findByClientId(ClientId.of("DESCONHECIDO"))).thenReturn(Optional.empty());

        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("DESCONHECIDO", "joao_silva", "senhaSegura123"));
        assertEquals(AuthenticationFailedException.GENERIC_MESSAGE, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoGenericaQuandoSistemaSemVinculoDeTenant() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoTenantInativo() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        Tenant inactiveTenant = activeTenant();
        inactiveTenant.deactivate();
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(inactiveTenant));
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(activeUser()));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(any(), any(), any()))
                .thenReturn(Optional.of(activeUserSystem()));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoSistemaInativo() {
        System inactiveSystem = activeSystem();
        inactiveSystem.deactivate();
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(inactiveSystem));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(activeTenant()));
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(activeUser()));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(any(), any(), any()))
                .thenReturn(Optional.of(activeUserSystem()));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoUsuarioNaoEncontrado() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(activeTenant()));
        when(userRepository.findByTenantIdAndUsername(any(), any())).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "inexistente", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoUsuarioBloqueado() {
        User blockedUser = activeUser();
        blockedUser.block();
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(activeTenant()));
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(blockedUser));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(any(), any(), any()))
                .thenReturn(Optional.of(activeUserSystem()));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoSemVinculoUsuarioSistema() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(activeTenant()));
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(activeUser()));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(any(), any(), any())).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoVinculoUsuarioSistemaBloqueado() {
        UserSystem blockedBinding = activeUserSystem();
        blockedBinding.block();
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(activeTenant()));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(any(), any(), any()))
                .thenReturn(Optional.of(blockedBinding));
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(activeUser()));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoGenericaParaSenhaIncorreta() {
        stubHappyPathUpTo(activeUser());
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(activeUser()));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaErrada"));
    }

    @Test
    void deveRegistrarTentativaFalhaESalvarUsuarioQuandoSenhaIncorreta() {
        User user = activeUser();
        stubHappyPathUpTo(user);
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(user));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaErrada"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getFailedLoginAttempts());
    }

    @Test
    void deveBloquearUsuarioAposAtingirLimiteDeTentativasFalhas() {
        User user = activeUser();
        for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS - 1; i++) {
            user.registerFailedLoginAttempt();
        }
        stubHappyPathUpTo(user);
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(user));

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("CRM_ACME", "joao_silva", "senhaErrada"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().isLocked());
    }

    @Test
    void deveZerarContadorDeTentativasAoAutenticarComSucesso() {
        User user = activeUser();
        user.registerFailedLoginAttempt();
        user.registerFailedLoginAttempt();
        stubHappyPathUpTo(user);
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(user));

        service.authenticate("CRM_ACME", "joao_silva", "senhaSegura123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getFailedLoginAttempts());
    }
}
