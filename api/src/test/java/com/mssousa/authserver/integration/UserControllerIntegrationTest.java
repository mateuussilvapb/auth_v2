package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.application.port.out.EmailSenderPort;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa {@code /admin/api/v1/tenants/{tenantId}/users} (seção 9 do plano) ponta a ponta.
 */
@AutoConfigureMockMvc
class UserControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private EmailSenderPort emailSender;

    @Test
    void deveCriarListarBuscarAtualizarEBloquearUsuario() throws Exception {
        Tenant tenant = createAndSaveTenant("initech");
        long tenantId = tenant.getId().value();

        String createResponseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"peter_gibbons","email":"peter@initech.com",
                                 "password":"senhaSegura123","name":"Peter Gibbons"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("peter_gibbons"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // Regressão: TSID excede Number.MAX_SAFE_INTEGER do JavaScript — "id"
                // precisa ser String no JSON, não number (ver Notas de PROGRESS.md).
                .andExpect(jsonPath("$.id").isString())
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(createResponseBody).get("id").asLong();

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantId + "/users/" + id).with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("peter@initech.com"));

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantId + "/users").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("peter_gibbons"));

        mockMvc.perform(put("/admin/api/v1/tenants/" + tenantId + "/users/" + id)
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Peter G.","email":"peterg@initech.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("peterg@initech.com"));

        mockMvc.perform(patch("/admin/api/v1/tenants/" + tenantId + "/users/" + id + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BLOCKED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void deveDispararResetDeSenhaAdministrativo() throws Exception {
        Tenant tenant = createAndSaveTenant("globex");
        long tenantId = tenant.getId().value();

        String createResponseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"milton_waddams","email":"milton@globex.com",
                                 "password":"senhaSegura123","name":"Milton Waddams"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(createResponseBody).get("id").asLong();

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users/" + id + "/reset-password")
                        .with(platformAdmin()))
                .andExpect(status().isOk());

        verify(emailSender).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("milton@globex.com"),
                org.mockito.ArgumentMatchers.eq("Milton Waddams"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarUsernameDuplicadoNoMesmoTenant() throws Exception {
        Tenant tenant = createAndSaveTenant("umbrella");
        long tenantId = tenant.getId().value();
        String body = """
                {"username":"bill_lumbergh","email":"bill@umbrella.com",
                 "password":"senhaSegura123","name":"Bill Lumbergh"}
                """;

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users")
                        .with(platformAdmin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users")
                        .with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bill_lumbergh","email":"outro@umbrella.com",
                                 "password":"senhaSegura123","name":"Outro Bill"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRetornar404ParaTenantInexistenteAoCriarUsuario() throws Exception {
        mockMvc.perform(post("/admin/api/v1/tenants/999999999/users")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"fantasma","email":"fantasma@example.com",
                                 "password":"senhaSegura123","name":"Fantasma"}
                                """))
                .andExpect(status().isNotFound());
    }
}
