package com.mssousa.authserver.application.service.user;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.out.EmailSenderPort;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
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
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private IdGeneratorPort idGenerator;
    @Mock
    private EmailSenderPort emailSender;

    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userRepository, tenantRepository, idGenerator, emailSender);
    }

    private Tenant existingTenant() {
        return Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build();
    }

    private User existingUser() {
        return User.builder()
                .id(UserId.of(1L)).tenantId(TenantId.of(1L))
                .username(Username.of("joao_silva")).email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123")).name("João da Silva").build();
    }

    @Test
    void deveCriarUsuarioEEnviarEmailDeBoasVindas() {
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(existingTenant()));
        when(userRepository.existsByTenantIdAndUsername(any(), any())).thenReturn(false);
        when(userRepository.existsByTenantIdAndEmail(any(), any())).thenReturn(false);
        when(idGenerator.generate()).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.createUser(TenantId.of(1L), "joao_silva", "joao@acme.com", "senhaSegura123", "João da Silva");

        assertEquals(Username.of("joao_silva"), created.getUsername());
        verify(emailSender).sendWelcomeEmail("joao@acme.com", "João da Silva");
    }

    @Test
    void deveLancarExcecaoAoCriarUsuarioParaTenantInexistente() {
        when(tenantRepository.findById(TenantId.of(99L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createUser(
                TenantId.of(99L), "joao_silva", "joao@acme.com", "senhaSegura123", "João"));
        verifyNoInteractions(emailSender);
    }

    @Test
    void deveLancarExcecaoAoCriarUsuarioComUsernameDuplicadoNoTenant() {
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(existingTenant()));
        when(userRepository.existsByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva"))).thenReturn(true);

        assertThrows(DomainException.class, () -> service.createUser(
                TenantId.of(1L), "joao_silva", "joao@acme.com", "senhaSegura123", "João"));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailSender);
    }

    @Test
    void deveAtualizarNomeEEmail() {
        User user = existingUser();
        when(userRepository.findByTenantIdAndId(TenantId.of(1L), UserId.of(1L))).thenReturn(Optional.of(user));
        when(userRepository.existsByTenantIdAndEmail(TenantId.of(1L), Email.of("novo@acme.com"))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = service.updateUser(TenantId.of(1L), UserId.of(1L), "Novo Nome", "novo@acme.com");
        assertEquals("Novo Nome", updated.getName());
        assertEquals(Email.of("novo@acme.com"), updated.getEmail());
    }

    @Test
    void deveBloquearEDesabilitarEAtivarUsuario() {
        User user = existingUser();
        when(userRepository.findByTenantIdAndId(TenantId.of(1L), UserId.of(1L))).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.blockUser(TenantId.of(1L), UserId.of(1L)).isBlocked());
        assertTrue(service.disableUser(TenantId.of(1L), UserId.of(1L)).isDisabled());
        assertTrue(service.activateUser(TenantId.of(1L), UserId.of(1L)).isActive());
    }

    @Test
    void deveLancarExcecaoAoBuscarUsuarioDeOutroTenant() {
        when(userRepository.findByTenantIdAndId(TenantId.of(2L), UserId.of(1L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getUser(TenantId.of(2L), UserId.of(1L)));
    }
}
