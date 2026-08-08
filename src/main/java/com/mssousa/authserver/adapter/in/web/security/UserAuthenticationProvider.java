package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.port.in.AuthenticateUserUseCase;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapta {@link AuthenticateUserUseCase} (Fase 5) ao SPI {@link AuthenticationProvider}
 * do Spring Security, para ser plugado no {@code AuthenticationManager} usado pelo
 * endpoint de login (Fase 7). Falha sempre vira {@link BadCredentialsException} com a
 * mesma mensagem genérica — nunca revela qual etapa da autenticação falhou.
 */
@Component
public class UserAuthenticationProvider implements AuthenticationProvider {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public UserAuthenticationProvider(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ClientAwareAuthenticationToken token = (ClientAwareAuthenticationToken) authentication;
        String usernameOrEmail = (String) token.getPrincipal();
        String password = (String) token.getCredentials();

        try {
            AuthenticatedUser authenticatedUser = authenticateUserUseCase.authenticate(
                    token.getClientId(), usernameOrEmail, password);
            return ClientAwareAuthenticationToken.authenticated(token.getClientId(), authenticatedUser, AUTHORITIES);
        } catch (AuthenticationFailedException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ClientAwareAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
