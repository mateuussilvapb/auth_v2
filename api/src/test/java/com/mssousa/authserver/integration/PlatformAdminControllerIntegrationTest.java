package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Autowired
    private PlatformAdminRepository platformAdminRepository;

    @Test
    void deveCriarListarEDesativarPlatformAdmin() throws Exception {
        // Precisa de um segundo admin ativo além do que a migration de seed (Fase 10, V15)
        // já garante existir — PlatformAdminPolicy bloqueia desativar o último ativo
        // (seção 2.1 do plano).
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
        // A migration de seed (Fase 10, V15) já deixa um platform admin ativo em toda base
        // nova — desativa qualquer um pré-existente para garantir que "unico_admin" (abaixo)
        // seja de fato o último ativo quando o teste tentar desativá-lo.
        for (PlatformAdmin existing : platformAdminRepository
                .findAll(PageRequest.of(0, 100)).getContent()) {
            if (existing.isActive()) {
                existing.deactivate();
                platformAdminRepository.save(existing);
            }
        }
        assertEquals(0, platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE));

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
