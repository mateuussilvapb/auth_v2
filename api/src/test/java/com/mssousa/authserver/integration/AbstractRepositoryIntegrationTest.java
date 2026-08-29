package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Base para testes de integração de repositório: contexto Spring completo sobre um
 * Postgres real (Testcontainers), cada teste roda numa transação revertida ao final —
 * não precisa de limpeza manual entre testes.
 */
@SpringBootTest
@Transactional
public abstract class AbstractRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected IdGeneratorPort idGenerator;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected SystemRepository systemRepository;

    @Autowired
    protected UserRepository userRepository;

    protected Tenant createAndSaveTenant(String code) {
        Tenant tenant = Tenant.builder()
                .id(TenantId.of(idGenerator.generate()))
                .code(TenantCode.of(code))
                .name(code + " Corp")
                .build();
        return tenantRepository.save(tenant);
    }

    protected System createAndSaveSystem(String clientId) {
        System system = System.builder()
                .id(SystemId.of(idGenerator.generate()))
                .clientId(ClientId.of(clientId))
                .name(clientId)
                .redirectUri(RedirectUri.of("https://" + clientId.toLowerCase() + ".example.com/callback"))
                .build();
        return systemRepository.save(system);
    }

    protected User createAndSaveUser(TenantId tenantId, String username, String email) {
        User user = User.builder()
                .id(UserId.of(idGenerator.generate()))
                .tenantId(tenantId)
                .username(Username.of(username))
                .email(Email.of(email))
                .password(Password.fromPlainText("senhaSegura123"))
                .name(username)
                .build();
        return userRepository.save(user);
    }

    /**
     * Simula um token de platform admin para os testes da API administrativa
     * (seção 9/Fase 8). {@code SecurityMockMvcRequestPostProcessors.jwt()} não aplica o
     * {@code PlatformAdminJwtAuthenticationConverter} real — a authority precisa ser
     * simulada explicitamente para refletir o que o converter produziria em produção.
     */
    protected RequestPostProcessor platformAdmin() {
        return jwt()
                .jwt(builder -> builder.claim("platform_admin", true).subject("1").expiresAt(Instant.now().plusSeconds(300)))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
    }
}
