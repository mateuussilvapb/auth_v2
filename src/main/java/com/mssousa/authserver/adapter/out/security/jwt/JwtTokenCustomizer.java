package com.mssousa.authserver.adapter.out.security.jwt;

import com.mssousa.authserver.application.model.AuthenticatedPlatformAdmin;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.model.AuthorizedUser;
import com.mssousa.authserver.application.port.in.AuthorizeUserUseCase;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Injeta os claims do access token (seção 7.2 do plano). Dois formatos, conforme o tipo
 * do principal autenticado: usuário de tenant (claims completos, incluindo
 * {@code profiles} — só códigos de perfil, nunca permissões, seção 1.2) ou platform
 * admin (sem tenant, usado para proteger {@code /admin/api/**}, seção 9).
 */
@Component
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final AuthorizeUserUseCase authorizeUserUseCase;
    private final TenantRepository tenantRepository;

    public JwtTokenCustomizer(AuthorizeUserUseCase authorizeUserUseCase, TenantRepository tenantRepository) {
        this.authorizeUserUseCase = authorizeUserUseCase;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }

        Authentication principalAuthentication = context.getPrincipal();
        Object principal = principalAuthentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            customizeForUser(context, authenticatedUser);
        } else if (principal instanceof AuthenticatedPlatformAdmin authenticatedPlatformAdmin) {
            customizeForPlatformAdmin(context, authenticatedPlatformAdmin);
        }
    }

    private void customizeForUser(JwtEncodingContext context, AuthenticatedUser authenticatedUser) {
        Tenant tenant = tenantRepository.findById(authenticatedUser.tenantId())
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant não encontrado ao emitir token: " + authenticatedUser.tenantId()));

        AuthorizedUser authorizedUser = authorizeUserUseCase.authorize(
                authenticatedUser.tenantId(), authenticatedUser.userId(), authenticatedUser.systemId());

        context.getClaims()
                .claim("tenant_id", authenticatedUser.tenantId().value().toString())
                .claim("tenant_code", tenant.getCode().value())
                .claim("client_id", context.getRegisteredClient().getClientId())
                .claim("username", authenticatedUser.username().value())
                .claim("email", authenticatedUser.email().value())
                .claim("name", authenticatedUser.name())
                .claim("profiles", authorizedUser.profileCodes());
    }

    private void customizeForPlatformAdmin(JwtEncodingContext context, AuthenticatedPlatformAdmin authenticatedPlatformAdmin) {
        context.getClaims()
                .claim("platform_admin", true)
                .claim("username", authenticatedPlatformAdmin.username().value())
                .claim("email", authenticatedPlatformAdmin.email().value())
                .claim("name", authenticatedPlatformAdmin.name());
    }
}
