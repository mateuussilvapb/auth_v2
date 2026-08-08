package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.model.AuthenticatedUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * {@code Authentication} carregando o {@code client_id} junto com as credenciais — o
 * login de usuário depende do tenant resolvido a partir dele (seção 7.1), algo que o
 * {@code UsernamePasswordAuthenticationToken} padrão do Spring Security não modela.
 * <p>
 * Não autenticado: {@code principal} é a string username/e-mail, {@code credentials} é a
 * senha em texto plano. Autenticado: {@code principal} é o {@link AuthenticatedUser}
 * (record de transporte da Fase 5), {@code credentials} é sempre {@code null}.
 * </p>
 */
public class ClientAwareAuthenticationToken extends AbstractAuthenticationToken {

    private final String clientId;
    private final Object principal;
    private final String credentials;

    private ClientAwareAuthenticationToken(String clientId, Object principal, String credentials,
                                            Collection<? extends GrantedAuthority> authorities, boolean authenticated) {
        super(authorities);
        this.clientId = clientId;
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(authenticated);
    }

    public static ClientAwareAuthenticationToken unauthenticated(String clientId, String usernameOrEmail, String password) {
        return new ClientAwareAuthenticationToken(clientId, usernameOrEmail, password, List.of(), false);
    }

    public static ClientAwareAuthenticationToken authenticated(String clientId, AuthenticatedUser authenticatedUser,
                                                                 Collection<? extends GrantedAuthority> authorities) {
        return new ClientAwareAuthenticationToken(clientId, authenticatedUser, null, authorities, true);
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    /**
     * {@code sub} do token quando autenticado (usado pelo gerador de token padrão do
     * Spring Authorization Server) — o ID do usuário, não o objeto inteiro.
     */
    @Override
    public String getName() {
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.userId().value().toString();
        }
        return String.valueOf(principal);
    }
}
