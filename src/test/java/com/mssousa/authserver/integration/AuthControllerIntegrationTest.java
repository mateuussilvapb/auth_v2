package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa {@code POST /api/auth/login} (seção 7.1 do plano) contra o
 * {@code AuthenticationManager} real, ponta a ponta (usuário + vínculos reais no Postgres
 * do Testcontainers). Mensagens de erro devem ser idênticas independente do motivo da
 * falha (seção 6.6/7.4) — client_id inexistente, usuário inexistente ou senha errada.
 */
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    private static final String GENERIC_ERROR = "Invalid credentials";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SystemTenantRepository systemTenantRepository;
    @Autowired
    private UserSystemRepository userSystemRepository;

    @Test
    void deveAutenticarComCredenciaisValidas() throws Exception {
        Tenant tenant = createAndSaveTenant("acme");
        System system = createAndSaveSystem("CRM_ACME_LOGIN");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "maria_souza", "maria@acme.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"maria_souza","password":"senhaSegura123"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria_souza"))
                .andExpect(jsonPath("$.name").value("maria_souza"));
    }

    @Test
    void deveRejeitarSenhaErradaComMensagemGenerica() throws Exception {
        Tenant tenant = createAndSaveTenant("globex");
        System system = createAndSaveSystem("CRM_GLOBEX_LOGIN");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "joao_costa", "joao@globex.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"joao_costa","password":"senhaErrada"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_ERROR));
    }

    @Test
    void deveRejeitarClientIdDesconhecidoComMesmaMensagemGenerica() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CLIENT_INEXISTENTE","usernameOrEmail":"qualquer","password":"qualquer123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_ERROR));
    }

    @Test
    void deveBloquearUsuarioAposExcederLimiteDeTentativasFalhas() throws Exception {
        Tenant tenant = createAndSaveTenant("umbrella");
        System system = createAndSaveSystem("CRM_UMBRELLA_LOGIN");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "ana_lima", "ana@umbrella.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());

        String wrongPasswordBody = """
                {"clientId":"%s","usernameOrEmail":"ana_lima","password":"senhaErrada"}
                """.formatted(system.getClientId().value());

        for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongPasswordBody))
                    .andExpect(status().isUnauthorized());
        }

        String correctPasswordBody = """
                {"clientId":"%s","usernameOrEmail":"ana_lima","password":"senhaSegura123"}
                """.formatted(system.getClientId().value());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPasswordBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_ERROR));
    }

    @Test
    void deveRejeitarRequisicaoComCamposEmBranco() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"","usernameOrEmail":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
