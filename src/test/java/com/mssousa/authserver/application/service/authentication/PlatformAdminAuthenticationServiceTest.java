package com.mssousa.authserver.application.service.authentication;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminAuthenticationServiceTest {

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    private PlatformAdminAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformAdminAuthenticationService(platformAdminRepository);
    }

    private PlatformAdmin activeAdmin() {
        return PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L)).username(Username.of("root_admin"))
                .email(Email.of("admin@seudominio.com")).password(Password.fromPlainText("senhaSegura123"))
                .name("Administrador").build();
    }

    @Test
    void deveAutenticarPorUsername() {
        when(platformAdminRepository.findByUsername(Username.of("root_admin"))).thenReturn(Optional.of(activeAdmin()));

        PlatformAdmin result = service.authenticate("root_admin", "senhaSegura123");
        assertEquals(Username.of("root_admin"), result.getUsername());
    }

    @Test
    void deveAutenticarPorEmailQuandoUsernameNaoResolve() {
        when(platformAdminRepository.findByEmail(Email.of("admin@seudominio.com"))).thenReturn(Optional.of(activeAdmin()));

        PlatformAdmin result = service.authenticate("admin@seudominio.com", "senhaSegura123");
        assertEquals(Email.of("admin@seudominio.com"), result.getEmail());
    }

    @Test
    void deveLancarExcecaoGenericaQuandoAdminNaoEncontrado() {
        when(platformAdminRepository.findByUsername(Username.of("inexistente"))).thenReturn(Optional.empty());

        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate("inexistente", "senhaSegura123"));
        assertEquals(AuthenticationFailedException.GENERIC_MESSAGE, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoGenericaParaSenhaIncorreta() {
        when(platformAdminRepository.findByUsername(Username.of("root_admin"))).thenReturn(Optional.of(activeAdmin()));

        assertThrows(AuthenticationFailedException.class, () -> service.authenticate("root_admin", "senhaErrada"));
    }

    @Test
    void deveLancarExcecaoGenericaQuandoAdminInativo() {
        PlatformAdmin inactive = activeAdmin();
        inactive.deactivate();
        when(platformAdminRepository.findByUsername(Username.of("root_admin"))).thenReturn(Optional.of(inactive));

        assertThrows(AuthenticationFailedException.class, () -> service.authenticate("root_admin", "senhaSegura123"));
    }
}
