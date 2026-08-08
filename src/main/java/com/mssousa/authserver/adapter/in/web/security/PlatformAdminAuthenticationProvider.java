package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.port.in.AuthenticatePlatformAdminUseCase;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapta {@link AuthenticatePlatformAdminUseCase} (Fase 5) ao SPI
 * {@link AuthenticationProvider}. Usa o {@code UsernamePasswordAuthenticationToken}
 * padrão — diferente do login de usuário, não há tenant/client_id envolvido (seção 2.1).
 */
@Component
public class PlatformAdminAuthenticationProvider implements AuthenticationProvider {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

    private final AuthenticatePlatformAdminUseCase authenticatePlatformAdminUseCase;

    public PlatformAdminAuthenticationProvider(AuthenticatePlatformAdminUseCase authenticatePlatformAdminUseCase) {
        this.authenticatePlatformAdminUseCase = authenticatePlatformAdminUseCase;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usernameOrEmail = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        try {
            PlatformAdmin admin = authenticatePlatformAdminUseCase.authenticate(usernameOrEmail, password);
            return UsernamePasswordAuthenticationToken.authenticated(admin, null, AUTHORITIES);
        } catch (AuthenticationFailedException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
