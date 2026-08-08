package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.port.in.AuthenticatePlatformAdminUseCase;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminAuthenticationProviderTest {

    @Mock
    private AuthenticatePlatformAdminUseCase authenticatePlatformAdminUseCase;

    private PlatformAdminAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PlatformAdminAuthenticationProvider(authenticatePlatformAdminUseCase);
    }

    private PlatformAdmin activeAdmin() {
        return PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L)).username(Username.of("root_admin"))
                .email(Email.of("admin@seudominio.com")).password(Password.fromPlainText("senhaSegura123"))
                .name("Administrador").build();
    }

    @Test
    void deveAutenticarERetornarTokenComPlatformAdminComoPrincipalEAuthorityCorreta() {
        when(authenticatePlatformAdminUseCase.authenticate("root_admin", "senhaSegura123")).thenReturn(activeAdmin());

        Authentication result = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("root_admin", "senhaSegura123"));

        assertTrue(result.isAuthenticated());
        assertInstanceOf(PlatformAdmin.class, result.getPrincipal());
        assertTrue(result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PLATFORM_ADMIN"::equals));
    }

    @Test
    void deveLancarBadCredentialsQuandoAutenticacaoFalha() {
        when(authenticatePlatformAdminUseCase.authenticate("root_admin", "senhaErrada"))
                .thenThrow(new AuthenticationFailedException());

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("root_admin", "senhaErrada")));
    }

    @Test
    void deveSuportarApenasUsernamePasswordAuthenticationToken() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(provider.supports(ClientAwareAuthenticationToken.class));
    }
}
