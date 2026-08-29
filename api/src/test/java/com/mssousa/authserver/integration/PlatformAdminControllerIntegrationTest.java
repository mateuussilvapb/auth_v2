package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa {@code /admin/api/v1/platform-admins} (seção 9 do plano) ponta a ponta.
 */
@AutoConfigureMockMvc
class PlatformAdminControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCriarListarEDesativarPlatformAdmin() throws Exception {
        // Precisa de um segundo admin ativo — PlatformAdminPolicy bloqueia desativar o
        // último ativo (seção 2.1 do plano), e este teste roda numa transação isolada
        // sem nenhum outro platform admin já existente.
        mockMvc.perform(post("/admin/api/v1/platform-admins")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin_reserva","email":"reserva@authserver.com",
                                 "password":"senhaSegura123","name":"Admin Reserva"}
                                """))
                .andExpect(status().isCreated());

        String createResponseBody = mockMvc.perform(post("/admin/api/v1/platform-admins")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"segundo_admin","email":"segundo@authserver.com",
                                 "password":"senhaSegura123","name":"Segundo Admin"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("segundo_admin"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(createResponseBody).get("id").asLong();

        mockMvc.perform(get("/admin/api/v1/platform-admins").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(patch("/admin/api/v1/platform-admins/" + id + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deveRejeitarDesativarUltimoPlatformAdminAtivo() throws Exception {
        String createResponseBody = mockMvc.perform(post("/admin/api/v1/platform-admins")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"unico_admin","email":"unico@authserver.com",
                                 "password":"senhaSegura123","name":"Único Admin"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(createResponseBody).get("id").asLong();

        mockMvc.perform(patch("/admin/api/v1/platform-admins/" + id + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Não é possível desativar o último platform admin ativo"));
    }

    @Test
    void deveRejeitarUsernameDuplicado() throws Exception {
        String body = """
                {"username":"admin_duplicado","email":"admin1@authserver.com",
                 "password":"senhaSegura123","name":"Admin Um"}
                """;

        mockMvc.perform(post("/admin/api/v1/platform-admins")
                        .with(platformAdmin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/api/v1/platform-admins")
                        .with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin_duplicado","email":"admin2@authserver.com",
                                 "password":"senhaSegura123","name":"Admin Dois"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRejeitarRequisicaoSemTokenDePlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/v1/platform-admins"))
                .andExpect(status().isUnauthorized());
    }
}
