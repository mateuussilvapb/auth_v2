package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.EmailSenderPort;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    @MockitoBean
    private EmailSenderPort emailSender;

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
    void deveInvalidarSessaoAoDeslogarImpedindoReautenticacaoSilenciosaNoAuthorize() throws Exception {
        Tenant tenant = createAndSaveTenant("wonka");
        System system = createAndSaveSystem("CRM_WONKA_LOGOUT");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "charlie_bucket", "charlie@wonka.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"charlie_bucket","password":"senhaSegura123"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "login deveria ter criado uma sessão");
        assertFalse(session.isInvalid());

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        assertTrue(session.isInvalid(), "logout deveria invalidar a sessão HTTP");

        String redirectUri = system.getRedirectUris().get(0).value();
        mockMvc.perform(get("/oauth2/authorize")
                        .accept(MediaType.TEXT_HTML)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", system.getClientId().value())
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("code_challenge", "abc123")
                        .queryParam("code_challenge_method", "S256")
                        .queryParam("state", "xyz")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/login?")));
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

    @Test
    void deveRetornarBrandingDoTenantParaClientIdValido() throws Exception {
        Tenant tenant = Tenant.builder()
                .id(TenantId.of(idGenerator.generate()))
                .code(TenantCode.of("wayne"))
                .name("Wayne Enterprises")
                .logoUrl("https://wayne.example.com/logo.png")
                .build();
        tenantRepository.save(tenant);
        System system = createAndSaveSystem("CRM_WAYNE_BRANDING");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());

        mockMvc.perform(get("/api/auth/branding").param("clientId", system.getClientId().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value("Wayne Enterprises"))
                .andExpect(jsonPath("$.logoUrl").value("https://wayne.example.com/logo.png"));
    }

    @Test
    void deveRetornar404ParaBrandingComClientIdDesconhecido() throws Exception {
        mockMvc.perform(get("/api/auth/branding").param("clientId", "CLIENT_INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveEnviarEmailDeRedefinicaoQuandoUsuarioExiste() throws Exception {
        Tenant tenant = createAndSaveTenant("stark");
        System system = createAndSaveSystem("CRM_STARK_FORGOT");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "tony_stark", "tony@stark.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"tony_stark"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordResetEmail(eq("tony@stark.com"),
                eq("tony_stark"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().contains("?token="));
    }

    @Test
    void naoDeveEnviarEmailQuandoUsuarioNaoExisteNemVazarIsso() throws Exception {
        System system = createAndSaveSystem("CRM_ANON_FORGOT");
        Tenant tenant = createAndSaveTenant("anon");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"ninguem@anon.com"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk());

        verifyNoInteractions(emailSender);
    }

    @Test
    void naoDeveEnviarEmailNemVazarClientIdDesconhecidoNoForgotPassword() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CLIENT_INEXISTENTE","usernameOrEmail":"qualquer"}
                                """))
                .andExpect(status().isOk());

        verifyNoInteractions(emailSender);
    }

    @Test
    void deveRedefinirSenhaComTokenValidoEPermitirLoginComNovaSenha() throws Exception {
        Tenant tenant = createAndSaveTenant("parker");
        System system = createAndSaveSystem("CRM_PARKER_RESET");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "peter_parker", "peter@parker.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"peter_parker"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordResetEmail(eq("peter@parker.com"),
                eq("peter_parker"), linkCaptor.capture());
        String link = linkCaptor.getValue();
        String token = link.substring(link.indexOf("?token=") + "?token=".length());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"novaSenhaSegura789"}
                                """.formatted(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"peter_parker","password":"novaSenhaSegura789"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk());
    }

    @Test
    void deveRejeitarTokenInvalidoComMensagemGenerica() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"tokeninvalidoquenaoexiste12345","newPassword":"novaSenhaSegura789"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de redefinição inválido ou expirado"));
    }
}
