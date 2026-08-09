package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa {@code /admin/api/v1/tenants/{tenantId}/systems} e {@code /admin/api/v1/systems}
 * (seção 9 do plano) ponta a ponta.
 */
@AutoConfigureMockMvc
class SystemControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private long createSystemAndReturnId(long tenantId) throws Exception {
        String responseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CRM_INITECH","name":"CRM Initech","publicClient":true,
                                 "initialRedirectUris":["https://crm.initech.com/callback"],"thirdParty":false}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(responseBody).get("id").asLong();
    }

    @Test
    void deveCriarListarAtualizarEDesativarSistema() throws Exception {
        Tenant tenant = createAndSaveTenant("initech");
        long systemId = createSystemAndReturnId(tenant.getId().value());

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenant.getId().value() + "/systems")
                        .with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].clientId").value("CRM_INITECH"));

        mockMvc.perform(put("/admin/api/v1/systems/" + systemId)
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"CRM Initech v2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CRM Initech v2"));

        mockMvc.perform(patch("/admin/api/v1/systems/" + systemId + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deveAdicionarERemoverRedirectUri() throws Exception {
        Tenant tenant = createAndSaveTenant("globex");
        long systemId = createSystemAndReturnId(tenant.getId().value());

        mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/redirect-uris")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uri":"https://crm.globex.com/callback2"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.redirectUris.length()").value(2));

        mockMvc.perform(delete("/admin/api/v1/systems/" + systemId + "/redirect-uris")
                        .with(platformAdmin())
                        .param("uri", "https://crm.globex.com/callback2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUris.length()").value(1));
    }

    @Test
    void naoDeveRemoverUltimaRedirectUri() throws Exception {
        Tenant tenant = createAndSaveTenant("wonka");
        long systemId = createSystemAndReturnId(tenant.getId().value());

        mockMvc.perform(delete("/admin/api/v1/systems/" + systemId + "/redirect-uris")
                        .with(platformAdmin())
                        .param("uri", "https://crm.initech.com/callback"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRotacionarSecretDeClientConfidencial() throws Exception {
        Tenant tenant = createAndSaveTenant("stark");
        String responseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenant.getId().value() + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CRM_STARK","name":"CRM Stark","publicClient":false,
                                 "clientSecret":"segredoInicial123",
                                 "initialRedirectUris":["https://crm.stark.com/callback"],"thirdParty":false}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long systemId = new ObjectMapper().readTree(responseBody).get("id").asLong();

        mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/rotate-secret")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newSecret":"segredoNovo456"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deveRejeitarClientIdDuplicado() throws Exception {
        Tenant tenant = createAndSaveTenant("umbrella");
        createSystemAndReturnId(tenant.getId().value());

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenant.getId().value() + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CRM_INITECH","name":"Outro sistema","publicClient":true,
                                 "initialRedirectUris":["https://outro.example.com/callback"],"thirdParty":false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404AoCriarSistemaParaTenantInexistente() throws Exception {
        mockMvc.perform(post("/admin/api/v1/tenants/999999999/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CRM_FANTASMA","name":"CRM Fantasma","publicClient":true,
                                 "initialRedirectUris":["https://fantasma.example.com/callback"],"thirdParty":false}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarCriacaoSemRedirectUris() throws Exception {
        Tenant tenant = createAndSaveTenant("acme");
        mockMvc.perform(post("/admin/api/v1/tenants/" + tenant.getId().value() + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"CRM_ACME","name":"CRM Acme","publicClient":true,
                                 "initialRedirectUris":[],"thirdParty":false}
                                """))
                .andExpect(status().isBadRequest());
    }
}
