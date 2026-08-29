package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.port.in.AuthenticateUserUseCase;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationProviderTest {

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    private UserAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new UserAuthenticationProvider(authenticateUserUseCase);
    }

    @Test
    void deveAutenticarERetornarTokenComAuthenticatedUserComoPrincipal() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                UserId.of(1L), TenantId.of(1L), SystemId.of(1L),
                Username.of("joao_silva"), Email.of("joao@acme.com"), "João da Silva");
        when(authenticateUserUseCase.authenticate("CRM_ACME", "joao_silva", "senhaSegura123"))
                .thenReturn(authenticatedUser);

        Authentication result = provider.authenticate(
                ClientAwareAuthenticationToken.unauthenticated("CRM_ACME", "joao_silva", "senhaSegura123"));

        assertTrue(result.isAuthenticated());
        assertEquals(authenticatedUser, result.getPrincipal());
        assertEquals("1", result.getName());
    }

    @Test
    void deveLancarBadCredentialsQuandoAutenticacaoFalha() {
        when(authenticateUserUseCase.authenticate("CRM_ACME", "joao_silva", "senhaErrada"))
                .thenThrow(new AuthenticationFailedException());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                ClientAwareAuthenticationToken.unauthenticated("CRM_ACME", "joao_silva", "senhaErrada")));
        assertEquals(AuthenticationFailedException.GENERIC_MESSAGE, exception.getMessage());
    }

    @Test
    void deveSuportarApenasClientAwareAuthenticationToken() {
        assertTrue(provider.supports(ClientAwareAuthenticationToken.class));
        assertFalse(provider.supports(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class));
    }
}
