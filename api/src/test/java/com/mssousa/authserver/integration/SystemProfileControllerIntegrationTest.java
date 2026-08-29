package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.domain.model.system.System;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa {@code /admin/api/v1/systems/{systemId}/profiles} (seção 9 do plano) ponta a
 * ponta.
 */
@AutoConfigureMockMvc
class SystemProfileControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCriarListarBuscarAtualizarEDesativarPerfil() throws Exception {
        System system = createAndSaveSystem("CRM_INITECH_PROFILES");
        long systemId = system.getId().value();

        String createResponseBody = mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ADMIN","description":"Administrador do CRM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // Regressão: TSID excede Number.MAX_SAFE_INTEGER do JavaScript — "id"
                // precisa ser String no JSON, não number (ver Notas de PROGRESS.md).
                .andExpect(jsonPath("$.id").isString())
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(createResponseBody).get("id").asLong();

        mockMvc.perform(get("/admin/api/v1/systems/" + systemId + "/profiles/" + id).with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN"));

        mockMvc.perform(get("/admin/api/v1/systems/" + systemId + "/profiles").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ADMIN"));

        mockMvc.perform(put("/admin/api/v1/systems/" + systemId + "/profiles/" + id)
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Administrador geral do CRM"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Administrador geral do CRM"));

        mockMvc.perform(patch("/admin/api/v1/systems/" + systemId + "/profiles/" + id + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deveRejeitarCodigoDePerfilDuplicadoNoMesmoSistema() throws Exception {
        System system = createAndSaveSystem("CRM_GLOBEX_PROFILES");
        long systemId = system.getId().value();

        mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"FINANCEIRO"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"FINANCEIRO"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRetornar404ParaSistemaInexistenteAoCriarPerfil() throws Exception {
        mockMvc.perform(post("/admin/api/v1/systems/999999999/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ADMIN"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarRequisicaoSemTokenDePlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/v1/systems/1/profiles"))
                .andExpect(status().isUnauthorized());
    }
}
