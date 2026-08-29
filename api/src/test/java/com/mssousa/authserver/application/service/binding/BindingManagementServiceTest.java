package com.mssousa.authserver.application.service.binding;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.application.port.out.UserSystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import com.mssousa.authserver.domain.service.TenantConsistencyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BindingManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemTenantRepository systemTenantRepository;
    @Mock
    private UserSystemRepository userSystemRepository;
    @Mock
    private SystemProfileRepository systemProfileRepository;
    @Mock
    private UserSystemProfileRepository userSystemProfileRepository;
    @Mock
    private IdGeneratorPort idGenerator;

    private BindingManagementService service;

    @BeforeEach
    void setUp() {
        service = new BindingManagementService(userRepository, systemTenantRepository, userSystemRepository,
                systemProfileRepository, userSystemProfileRepository, idGenerator, new TenantConsistencyValidator());
    }

    private User userOfTenant(long tenantId) {
        return User.builder()
                .id(UserId.of(1L)).tenantId(TenantId.of(tenantId))
                .username(Username.of("joao_silva")).email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123")).name("João").build();
    }

    private SystemTenant systemTenantOf(long tenantId) {
        return SystemTenant.builder().id(SystemTenantId.of(1L)).tenantId(TenantId.of(tenantId)).systemId(SystemId.of(1L)).build();
    }

    @Test
    void deveVincularUsuarioAoSistemaQuandoMesmoTenant() {
        when(userRepository.findByTenantIdAndId(TenantId.of(1L), UserId.of(1L))).thenReturn(Optional.of(userOfTenant(1L)));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(systemTenantOf(1L)));
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(any(), any(), any())).thenReturn(Optional.empty());
        when(idGenerator.generate()).thenReturn(100L);
        when(userSystemRepository.save(any(UserSystem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSystem binding = service.bindUserToSystem(TenantId.of(1L), UserId.of(1L), SystemId.of(1L));

        assertTrue(binding.isActive());
        assertEquals(TenantId.of(1L), binding.getTenantId());
    }

    @Test
    void deveLancarExcecaoAoVincularUsuarioDeOutroTenantAoSistema() {
        when(userRepository.findByTenantIdAndId(TenantId.of(2L), UserId.of(1L))).thenReturn(Optional.of(userOfTenant(2L)));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(systemTenantOf(1L)));

        DomainException exception = assertThrows(DomainException.class,
                () -> service.bindUserToSystem(TenantId.of(2L), UserId.of(1L), SystemId.of(1L)));
        assertEquals(TenantConsistencyValidator.ERROR_TENANT_MISMATCH, exception.getMessage());
        verify(userSystemRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoVincularUsuarioJaVinculado() {
        when(userRepository.findByTenantIdAndId(TenantId.of(1L), UserId.of(1L))).thenReturn(Optional.of(userOfTenant(1L)));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(systemTenantOf(1L)));
        UserSystem existente = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(Optional.of(existente));

        assertThrows(DomainException.class, () -> service.bindUserToSystem(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)));
        verify(userSystemRepository, never()).save(any());
    }

    @Test
    void deveVincularPerfilQuandoPerfilPertenceAoSistemaDoVinculo() {
        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();
        SystemProfile profile = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();

        when(userSystemRepository.findByTenantIdAndId(TenantId.of(1L), UserSystemId.of(1L))).thenReturn(Optional.of(userSystem));
        when(systemProfileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(1L))).thenReturn(Optional.of(profile));
        when(userSystemProfileRepository.findByUserSystemIdAndSystemProfileId(any(), any())).thenReturn(Optional.empty());
        when(idGenerator.generate()).thenReturn(200L);
        when(userSystemProfileRepository.save(any(UserSystemProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSystemProfile binding = service.bindProfileToUserSystem(TenantId.of(1L), UserSystemId.of(1L), SystemProfileId.of(1L));

        assertTrue(binding.isActive());
        assertEquals(SystemProfileId.of(1L), binding.getSystemProfileId());
    }

    @Test
    void deveLancarExcecaoAoVincularPerfilDeOutroSistema() {
        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();

        when(userSystemRepository.findByTenantIdAndId(TenantId.of(1L), UserSystemId.of(1L))).thenReturn(Optional.of(userSystem));
        when(systemProfileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(99L))).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> service.bindProfileToUserSystem(TenantId.of(1L), UserSystemId.of(1L), SystemProfileId.of(99L)));
        verify(userSystemProfileRepository, never()).save(any());
    }

    @Test
    void deveAtivarDesativarEBloquearVinculoUsuarioSistema() {
        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();
        when(userSystemRepository.findByTenantIdAndId(TenantId.of(1L), UserSystemId.of(1L))).thenReturn(Optional.of(userSystem));
        when(userSystemRepository.save(any(UserSystem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.deactivateUserSystem(TenantId.of(1L), UserSystemId.of(1L)).isInactive());
        assertTrue(service.blockUserSystem(TenantId.of(1L), UserSystemId.of(1L)).isBlocked());
        assertTrue(service.activateUserSystem(TenantId.of(1L), UserSystemId.of(1L)).isActive());
    }

    @Test
    void deveLancarExcecaoAoOperarVinculoInexistente() {
        when(userSystemRepository.findByTenantIdAndId(TenantId.of(1L), UserSystemId.of(99L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.activateUserSystem(TenantId.of(1L), UserSystemId.of(99L)));
    }

    @Test
    void deveListarVinculosUsuarioSistemaDoTenant() {
        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(userSystemRepository.findByTenantIdAndUserId(TenantId.of(1L), UserId.of(1L), pageable))
                .thenReturn(new PageImpl<>(List.of(userSystem)));

        Page<UserSystem> result = service.listUserSystems(TenantId.of(1L), UserId.of(1L), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(UserSystemId.of(1L), result.getContent().get(0).getId());
    }

    @Test
    void deveListarPerfisDeUmVinculoUsuarioSistemaExistente() {
        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();
        UserSystemProfile profileBinding = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(1L)).userSystemId(UserSystemId.of(1L)).systemProfileId(SystemProfileId.of(1L)).build();
        when(userSystemRepository.findByTenantIdAndId(TenantId.of(1L), UserSystemId.of(1L))).thenReturn(Optional.of(userSystem));
        when(userSystemProfileRepository.findByUserSystemId(UserSystemId.of(1L))).thenReturn(List.of(profileBinding));

        List<UserSystemProfile> result = service.listUserSystemProfiles(TenantId.of(1L), UserSystemId.of(1L));

        assertEquals(1, result.size());
    }

    @Test
    void deveLancarExcecaoAoListarPerfisDeVinculoDeOutroTenant() {
        when(userSystemRepository.findByTenantIdAndId(TenantId.of(1L), UserSystemId.of(1L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.listUserSystemProfiles(TenantId.of(1L), UserSystemId.of(1L)));
    }
}
