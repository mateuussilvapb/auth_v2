package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientAwareAuthenticationTokenTest {

    @Test
    void tokenNaoAutenticadoExpoeUsernameOuEmailESenhaComoCredenciais() {
        ClientAwareAuthenticationToken token = ClientAwareAuthenticationToken.unauthenticated(
                "CRM_ACME", "joao_silva", "senhaSegura123");

        assertFalse(token.isAuthenticated());
        assertEquals("CRM_ACME", token.getClientId());
        assertEquals("joao_silva", token.getPrincipal());
        assertEquals("senhaSegura123", token.getCredentials());
        assertEquals("joao_silva", token.getName());
    }

    @Test
    void tokenAutenticadoExpoeAuthenticatedUserSemCredenciais() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                UserId.of(1L), TenantId.of(1L), SystemId.of(1L),
                Username.of("joao_silva"), Email.of("joao@acme.com"), "João da Silva");

        ClientAwareAuthenticationToken token = ClientAwareAuthenticationToken.authenticated(
                "CRM_ACME", authenticatedUser, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertTrue(token.isAuthenticated());
        assertEquals(authenticatedUser, token.getPrincipal());
        assertNull(token.getCredentials());
        assertEquals("1", token.getName());
    }
}
