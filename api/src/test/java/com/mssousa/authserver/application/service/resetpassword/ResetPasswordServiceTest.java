package com.mssousa.authserver.application.service.resetpassword;

import com.mssousa.authserver.application.port.out.EmailSenderPort;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.PasswordResetTokenRepository;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.ResetTokenValue;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemTenantRepository systemTenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private IdGeneratorPort idGenerator;
    @Mock
    private EmailSenderPort emailSender;

    private ResetPasswordService service;

    @BeforeEach
    void setUp() {
        service = new ResetPasswordService(systemRepository, systemTenantRepository, userRepository,
                passwordResetTokenRepository, idGenerator, emailSender,
                "https://auth.seudominio.com/reset-password");
    }

    private User existingUser() {
        return User.builder()
                .id(UserId.of(1L)).tenantId(TenantId.of(1L))
                .username(Username.of("joao_silva")).email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123")).name("João da Silva").build();
    }

    private System activeSystem() {
        return System.builder().id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build();
    }

    private SystemTenant activeSystemTenant() {
        return SystemTenant.builder().id(SystemTenantId.of(1L)).tenantId(TenantId.of(1L)).systemId(SystemId.of(1L)).build();
    }

    private void stubClientResolution() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
    }

    @Test
    void deveGerarTokenEEnviarEmailQuandoUsuarioExiste() {
        stubClientResolution();
        when(userRepository.findByTenantIdAndUsername(TenantId.of(1L), Username.of("joao_silva")))
                .thenReturn(Optional.of(existingUser()));
        when(idGenerator.generate()).thenReturn(1L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.requestReset("CRM_ACME", "joao_silva");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordResetEmail(eq("joao@acme.com"), eq("João da Silva"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("https://auth.seudominio.com/reset-password?token="));
    }

    @Test
    void naoDeveLancarExcecaoNemEnviarEmailQuandoUsuarioNaoExiste() {
        stubClientResolution();
        when(userRepository.findByTenantIdAndEmail(any(), any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requestReset("CRM_ACME", "inexistente@acme.com"));
        verifyNoInteractions(emailSender);
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void naoDeveLancarExcecaoNemEnviarEmailQuandoClientIdDesconhecido() {
        when(systemRepository.findByClientId(ClientId.of("DESCONHECIDO"))).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requestReset("DESCONHECIDO", "joao_silva"));
        verifyNoInteractions(emailSender);
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void deveEnviarEmailAoResetarSenhaViaAdmin() {
        when(userRepository.findByTenantIdAndId(TenantId.of(1L), UserId.of(1L)))
                .thenReturn(Optional.of(existingUser()));
        when(idGenerator.generate()).thenReturn(1L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.requestResetForUser(TenantId.of(1L), UserId.of(1L));

        verify(emailSender).sendPasswordResetEmail(eq("joao@acme.com"), eq("João da Silva"), any());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontradoNoResetViaAdmin() {
        when(userRepository.findByTenantIdAndId(TenantId.of(1L), UserId.of(999L))).thenReturn(Optional.empty());

        assertThrows(com.mssousa.authserver.application.exception.ResourceNotFoundException.class,
                () -> service.requestResetForUser(TenantId.of(1L), UserId.of(999L)));
        verifyNoInteractions(emailSender);
    }

    @Test
    void deveConfirmarRedefinicaoComTokenValido() {
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(1L), UserId.of(1L), Instant.now().plus(30, ChronoUnit.MINUTES));
        when(passwordResetTokenRepository.findByValue(any(ResetTokenValue.class))).thenReturn(Optional.of(generated.token()));
        when(userRepository.findById(UserId.of(1L))).thenReturn(Optional.of(existingUser()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmReset(generated.rawValue(), "novaSenhaSegura456");

        verify(userRepository).save(argThat(user -> user.verifyPassword("novaSenhaSegura456")));
        assertTrue(generated.token().isUsed());
    }

    @Test
    void deveLancarExcecaoGenericaParaTokenInexistente() {
        when(passwordResetTokenRepository.findByValue(any(ResetTokenValue.class))).thenReturn(Optional.empty());

        DomainException exception = assertThrows(DomainException.class,
                () -> service.confirmReset("a".repeat(40), "novaSenhaSegura456"));
        assertEquals(ResetPasswordService.ERROR_INVALID_TOKEN, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoGenericaParaTokenExpirado() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(PasswordResetTokenId.of(1L))
                .value(ResetTokenValue.ofRawToken("a".repeat(40)))
                .userId(UserId.of(1L))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByValue(any(ResetTokenValue.class))).thenReturn(Optional.of(expiredToken));

        DomainException exception = assertThrows(DomainException.class,
                () -> service.confirmReset("a".repeat(40), "novaSenhaSegura456"));
        assertEquals(ResetPasswordService.ERROR_INVALID_TOKEN, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoGenericaParaTokenJaUtilizado() {
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .id(PasswordResetTokenId.of(1L))
                .value(ResetTokenValue.ofRawToken("a".repeat(40)))
                .userId(UserId.of(1L))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .used(true)
                .build();
        when(passwordResetTokenRepository.findByValue(any(ResetTokenValue.class))).thenReturn(Optional.of(usedToken));

        assertThrows(DomainException.class, () -> service.confirmReset("a".repeat(40), "novaSenhaSegura456"));
    }

    @Test
    void deveLancarExcecaoGenericaParaTokenEmFormatoInvalido() {
        DomainException exception = assertThrows(DomainException.class,
                () -> service.confirmReset("curto", "novaSenhaSegura456"));
        assertEquals(ResetPasswordService.ERROR_INVALID_TOKEN, exception.getMessage());
        verifyNoInteractions(passwordResetTokenRepository);
    }
}
