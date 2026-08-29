package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa {@code /admin/api/v1/tenants} (seção 9 do plano) ponta a ponta: exige token de
 * platform admin (claim {@code platform_admin}) e persiste no Postgres real.
 */
@AutoConfigureMockMvc
class TenantControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCriarListarBuscarAtualizarEDesativarTenant() throws Exception {
        String createBody = """
                {"code":"initech","name":"Initech"}
                """;

        String createResponseBody = mockMvc.perform(post("/admin/api/v1/tenants")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("initech"))
                .andExpect(jsonPath("$.name").value("Initech"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // Regressão: "id" precisa ser String no JSON, não number — um TSID
                // (seção 4.2 do plano) regularmente excede Number.MAX_SAFE_INTEGER do
                // JavaScript; serializado como número, o console Angular perde precisão
                // ao decodificar (confirmado num teste manual: um ID virou outro ID ao
                // fazer round-trip pelo JSON.parse do browser), quebrando toda operação
                // subsequente que dependa do ID exato.
                .andExpect(jsonPath("$.id").isString())
                .andReturn().getResponse().getContentAsString();

        JsonNode createResponse = new ObjectMapper().readTree(createResponseBody);
        long id = createResponse.get("id").asLong();

        mockMvc.perform(get("/admin/api/v1/tenants/" + id).with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("initech"));

        mockMvc.perform(get("/admin/api/v1/tenants").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(put("/admin/api/v1/tenants/" + id)
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Initech Corp"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Initech Corp"));

        mockMvc.perform(patch("/admin/api/v1/tenants/" + id + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deveRejeitarRequisicaoSemTokenDePlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/v1/tenants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarTokenSemClaimDePlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/v1/tenants")
                        .with(jwt().jwt(builder -> builder.subject("1").expiresAt(Instant.now().plusSeconds(300)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRejeitarCodigoDeTenantDuplicado() throws Exception {
        mockMvc.perform(post("/admin/api/v1/tenants")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"acme-dup","name":"Acme"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/api/v1/tenants")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"acme-dup","name":"Acme outra vez"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Já existe um tenant com o código 'acme-dup'"));
    }

    @Test
    void deveRetornar404ParaTenantInexistente() throws Exception {
        mockMvc.perform(get("/admin/api/v1/tenants/999999999").with(platformAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarCriacaoComCodigoEmBranco() throws Exception {
        mockMvc.perform(post("/admin/api/v1/tenants")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"","name":"Sem código"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists());
    }
}
