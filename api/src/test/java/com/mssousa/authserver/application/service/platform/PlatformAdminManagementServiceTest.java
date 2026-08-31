package com.mssousa.authserver.application.service.platform;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import com.mssousa.authserver.domain.service.PlatformAdminPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAdminManagementServiceTest {

    @Mock
    private PlatformAdminRepository platformAdminRepository;
    @Mock
    private IdGeneratorPort idGenerator;

    private PlatformAdminManagementService service;

    @BeforeEach
    void setUp() {
        service = new PlatformAdminManagementService(platformAdminRepository, idGenerator, new PlatformAdminPolicy());
    }

    private PlatformAdmin existingAdmin() {
        return PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L))
                .username(Username.of("root_admin"))
                .email(Email.of("root@authserver.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("Root Admin")
                .build();
    }

    @Test
    void deveCriarPlatformAdminQuandoUsernameEEmailDisponiveis() {
        when(idGenerator.generate()).thenReturn(1L);
        when(platformAdminRepository.existsByUsername(Username.of("root_admin"))).thenReturn(false);
        when(platformAdminRepository.existsByEmail(Email.of("root@authserver.com"))).thenReturn(false);
        when(platformAdminRepository.save(any(PlatformAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformAdmin created = service.createPlatformAdmin(
                "root_admin", "root@authserver.com", "senhaSegura123", "Root Admin");

        assertEquals(Username.of("root_admin"), created.getUsername());
        assertTrue(created.isActive());
        verify(platformAdminRepository).save(any(PlatformAdmin.class));
    }

    @Test
    void deveLancarExcecaoAoCriarComUsernameJaExistente() {
        when(platformAdminRepository.existsByUsername(Username.of("root_admin"))).thenReturn(true);

        DomainException exception = assertThrows(DomainException.class, () -> service.createPlatformAdmin(
                "root_admin", "outro@authserver.com", "senhaSegura123", "Outro Admin"));
        assertTrue(exception.getMessage().contains("root_admin"));
        verify(platformAdminRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCriarComEmailJaExistente() {
        when(platformAdminRepository.existsByUsername(Username.of("outro_admin"))).thenReturn(false);
        when(platformAdminRepository.existsByEmail(Email.of("root@authserver.com"))).thenReturn(true);

        DomainException exception = assertThrows(DomainException.class, () -> service.createPlatformAdmin(
                "outro_admin", "root@authserver.com", "senhaSegura123", "Outro Admin"));
        assertTrue(exception.getMessage().contains("root@authserver.com"));
        verify(platformAdminRepository, never()).save(any());
    }

    @Test
    void deveAtivarPlatformAdmin() {
        PlatformAdmin admin = existingAdmin();
        admin.deactivate();
        when(platformAdminRepository.findById(PlatformAdminId.of(1L))).thenReturn(Optional.of(admin));
        when(platformAdminRepository.save(any(PlatformAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformAdmin activated = service.activatePlatformAdmin(PlatformAdminId.of(1L));
        assertTrue(activated.isActive());
    }

    @Test
    void deveDesativarPlatformAdminQuandoHaOutrosAtivos() {
        PlatformAdmin admin = existingAdmin();
        when(platformAdminRepository.findById(PlatformAdminId.of(1L))).thenReturn(Optional.of(admin));
        when(platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE)).thenReturn(2L);
        when(platformAdminRepository.save(any(PlatformAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformAdmin deactivated = service.deactivatePlatformAdmin(PlatformAdminId.of(1L));
        assertFalse(deactivated.isActive());
    }

    @Test
    void deveLancarExcecaoAoDesativarUltimoPlatformAdminAtivo() {
        PlatformAdmin admin = existingAdmin();
        when(platformAdminRepository.findById(PlatformAdminId.of(1L))).thenReturn(Optional.of(admin));
        when(platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE)).thenReturn(1L);

        DomainException exception = assertThrows(DomainException.class,
                () -> service.deactivatePlatformAdmin(PlatformAdminId.of(1L)));
        assertEquals(PlatformAdminPolicy.ERROR_LAST_ACTIVE_ADMIN, exception.getMessage());
        verify(platformAdminRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAtivarPlatformAdminInexistente() {
        when(platformAdminRepository.findById(PlatformAdminId.of(99L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.activatePlatformAdmin(PlatformAdminId.of(99L)));
    }

    @Test
    void deveTrocarAPropriaSenhaQuandoSenhaAtualCorreta() {
        PlatformAdmin admin = PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L))
                .username(Username.of("admin"))
                .email(Email.of("admin@example.com"))
                .password(Password.fromPlainText("senhaTemporaria"))
                .name("Administrador")
                .mustChangePassword(true)
                .build();
        when(platformAdminRepository.findById(PlatformAdminId.of(1L))).thenReturn(Optional.of(admin));
        when(platformAdminRepository.save(any(PlatformAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformAdmin changed = service.changeOwnPassword(PlatformAdminId.of(1L), "senhaTemporaria", "novaSenhaSegura");

        assertFalse(changed.mustChangePassword());
        assertTrue(changed.verifyPassword("novaSenhaSegura"));
    }

    @Test
    void deveLancarExcecaoAoTrocarAPropriaSenhaComSenhaAtualIncorreta() {
        PlatformAdmin admin = existingAdmin();
        when(platformAdminRepository.findById(PlatformAdminId.of(1L))).thenReturn(Optional.of(admin));

        DomainException exception = assertThrows(DomainException.class,
                () -> service.changeOwnPassword(PlatformAdminId.of(1L), "senhaErrada", "novaSenhaSegura"));
        assertEquals("Senha atual incorreta", exception.getMessage());
        verify(platformAdminRepository, never()).save(any());
    }

    @Test
    void deveListarPlatformAdmins() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(platformAdminRepository.findAll(pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(existingAdmin())));

        assertEquals(1, service.listPlatformAdmins(pageable).getTotalElements());
    }
}
