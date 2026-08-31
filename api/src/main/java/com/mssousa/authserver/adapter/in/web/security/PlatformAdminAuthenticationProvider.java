package com.mssousa.authserver.adapter.in.web.security;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedPlatformAdmin;
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
 * {@link AuthenticationProvider}. Suporta tanto {@code UsernamePasswordAuthenticationToken}
 * quanto {@link ClientAwareAuthenticationToken} — o segundo é o que
 * {@code POST /api/auth/login} de fato constrói (Fase 7/9): não há tenant/client_id
 * envolvido na autenticação de platform admin (seção 2.1), então o {@code clientId}
 * carregado pelo token é simplesmente ignorado aqui. É o fallback do
 * {@code AuthenticationManager} (Fase 6, {@code ProviderManager}) — {@link
 * UserAuthenticationProvider} tenta primeiro; quando falha (client_id desconhecido ou
 * usuário inexistente no tenant), o {@code ProviderManager} cai para este provider, que
 * tenta a mesma combinação usuário/senha como platform admin. É assim que o console
 * administrativo Angular (client OAuth2 estático, não vinculado a tenant — ver
 * {@code RegisteredClientRepositoryConfig}) autentica platform admins pelo mesmo endpoint
 * de login usado pelos usuários de tenant.
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
            // Principal é um record de transporte, não o agregado PlatformAdmin inteiro —
            // este Authentication acaba persistido em OAuth2Authorization.attributes
            // (JdbcOAuth2AuthorizationService) entre /api/auth/login e a troca de código
            // por token; carregar o hash de senha do agregado até lá não teria propósito e
            // exigiria um mixin Jackson mais complexo (ver OAuth2AuthorizationJsonMapperFactory).
            AuthenticatedPlatformAdmin authenticatedPlatformAdmin = new AuthenticatedPlatformAdmin(
                    admin.getId(), admin.getUsername(), admin.getEmail(), admin.getName(), admin.mustChangePassword());
            return UsernamePasswordAuthenticationToken.authenticated(authenticatedPlatformAdmin, null, AUTHORITIES);
        } catch (AuthenticationFailedException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)
                || ClientAwareAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
