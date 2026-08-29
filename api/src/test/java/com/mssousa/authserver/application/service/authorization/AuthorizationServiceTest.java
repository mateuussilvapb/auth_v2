package com.mssousa.authserver.application.service.authorization;

import com.mssousa.authserver.application.exception.AccessDeniedException;
import com.mssousa.authserver.application.model.AuthorizedUser;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.service.AccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserSystemRepository userSystemRepository;
    @Mock
    private UserSystemProfileRepository userSystemProfileRepository;
    @Mock
    private SystemProfileRepository systemProfileRepository;

    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(userSystemRepository, userSystemProfileRepository,
                systemProfileRepository, new AccessValidator());
    }

    private UserSystem activeUserSystem() {
        return UserSystem.builder().id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L))
                .tenantId(TenantId.of(1L)).build();
    }

    @Test
    void deveRetornarApenasCodigosDePerfisAtivos() {
        UserSystem userSystem = activeUserSystem();
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(Optional.of(userSystem));

        UserSystemProfile activeBinding = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(1L)).userSystemId(UserSystemId.of(1L)).systemProfileId(SystemProfileId.of(1L)).build();
        UserSystemProfile inactiveBinding = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(2L)).userSystemId(UserSystemId.of(1L)).systemProfileId(SystemProfileId.of(2L))
                .build();
        inactiveBinding.deactivate();

        when(userSystemProfileRepository.findByUserSystemId(UserSystemId.of(1L)))
                .thenReturn(List.of(activeBinding, inactiveBinding));

        SystemProfile adminProfile = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();
        SystemProfile financeiroProfile = SystemProfile.builder()
                .id(SystemProfileId.of(2L)).systemId(SystemId.of(1L)).code(ProfileCode.of("FINANCEIRO")).build();

        when(systemProfileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(1L)))
                .thenReturn(Optional.of(adminProfile));
        when(systemProfileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(2L)))
                .thenReturn(Optional.of(financeiroProfile));

        AuthorizedUser result = service.authorize(TenantId.of(1L), UserId.of(1L), SystemId.of(1L));

        assertEquals(List.of("ADMIN"), result.profileCodes());
    }

    @Test
    void deveExcluirPerfilComProfileEntityInativo() {
        UserSystem userSystem = activeUserSystem();
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(Optional.of(userSystem));

        UserSystemProfile binding = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(1L)).userSystemId(UserSystemId.of(1L)).systemProfileId(SystemProfileId.of(1L)).build();
        when(userSystemProfileRepository.findByUserSystemId(UserSystemId.of(1L))).thenReturn(List.of(binding));

        SystemProfile inactiveProfile = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();
        inactiveProfile.deactivate();
        when(systemProfileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(1L)))
                .thenReturn(Optional.of(inactiveProfile));

        AuthorizedUser result = service.authorize(TenantId.of(1L), UserId.of(1L), SystemId.of(1L));
        assertTrue(result.profileCodes().isEmpty());
    }

    @Test
    void deveRetornarListaVaziaQuandoUsuarioSemPerfis() {
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(Optional.of(activeUserSystem()));
        when(userSystemProfileRepository.findByUserSystemId(UserSystemId.of(1L))).thenReturn(List.of());

        AuthorizedUser result = service.authorize(TenantId.of(1L), UserId.of(1L), SystemId.of(1L));
        assertTrue(result.profileCodes().isEmpty());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioSemVinculoComSistema() {
        when(userSystemRepository.findByTenantIdAndUserIdAndSystemId(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.authorize(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)));
    }
}
