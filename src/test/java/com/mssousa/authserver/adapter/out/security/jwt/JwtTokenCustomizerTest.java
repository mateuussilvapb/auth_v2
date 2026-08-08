package com.mssousa.authserver.adapter.out.security.jwt;

import com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.model.AuthorizedUser;
import com.mssousa.authserver.application.port.in.AuthorizeUserUseCase;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenCustomizerTest {

    @Mock
    private AuthorizeUserUseCase authorizeUserUseCase;
    @Mock
    private TenantRepository tenantRepository;

    private JwtTokenCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new JwtTokenCustomizer(authorizeUserUseCase, tenantRepository);
    }

    private RegisteredClient registeredClient() {
        return RegisteredClient.withId("1")
                .clientId("CRM_ACME")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://crm.acme.com/callback")
                .build();
    }

    private JwtEncodingContext.Builder contextBuilder(Authentication principal, OAuth2TokenType tokenType) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject(principal.getName());
        return JwtEncodingContext.with(JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256), claims)
                .registeredClient(registeredClient())
                .principal(principal)
                .tokenType(tokenType);
    }

    @Test
    void deveAdicionarClaimsCompletosParaUsuarioDeTenant() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                UserId.of(1L), TenantId.of(1L), SystemId.of(1L),
                Username.of("joao_silva"), Email.of("joao@acme.com"), "João da Silva");
        Authentication principal = ClientAwareAuthenticationToken.authenticated("CRM_ACME", authenticatedUser,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(tenantRepository.findById(TenantId.of(1L)))
                .thenReturn(Optional.of(Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build()));
        when(authorizeUserUseCase.authorize(TenantId.of(1L), UserId.of(1L), SystemId.of(1L)))
                .thenReturn(new AuthorizedUser(UserId.of(1L), TenantId.of(1L), SystemId.of(1L), List.of("ADMIN", "FINANCEIRO")));

        JwtEncodingContext context = contextBuilder(principal, OAuth2TokenType.ACCESS_TOKEN).build();
        customizer.customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        assertEquals("1", claims.getClaim("tenant_id"));
        assertEquals("acme", claims.getClaim("tenant_code"));
        assertEquals("CRM_ACME", claims.getClaim("client_id"));
        assertEquals("joao_silva", claims.getClaim("username"));
        assertEquals("joao@acme.com", claims.getClaim("email"));
        assertEquals("João da Silva", claims.getClaim("name"));
        assertEquals(List.of("ADMIN", "FINANCEIRO"), claims.getClaim("profiles"));
    }

    @Test
    void deveAdicionarClaimsSemTenantParaPlatformAdmin() {
        PlatformAdmin admin = PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L)).username(Username.of("root_admin"))
                .email(Email.of("admin@seudominio.com")).password(Password.fromPlainText("senhaSegura123"))
                .name("Administrador").build();
        Authentication principal = UsernamePasswordAuthenticationToken.authenticated(admin, null,
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));

        JwtEncodingContext context = contextBuilder(principal, OAuth2TokenType.ACCESS_TOKEN).build();
        customizer.customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        assertEquals(Boolean.TRUE, claims.getClaim("platform_admin"));
        assertEquals("root_admin", claims.getClaim("username"));
        assertNull(claims.getClaim("tenant_id"));
        assertNull(claims.getClaim("profiles"));
    }

    @Test
    void naoDeveCustomizarTokenQueNaoSejaAccessToken() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                UserId.of(1L), TenantId.of(1L), SystemId.of(1L),
                Username.of("joao_silva"), Email.of("joao@acme.com"), "João da Silva");
        Authentication principal = ClientAwareAuthenticationToken.authenticated("CRM_ACME", authenticatedUser, List.of());

        JwtEncodingContext context = contextBuilder(principal, OAuth2TokenType.REFRESH_TOKEN).build();
        customizer.customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        assertNull(claims.getClaim("tenant_id"));
    }
}
