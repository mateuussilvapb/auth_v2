package com.mssousa.authserver.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a documentação OpenAPI (seção 9/Fase 8 do plano): exige token de platform admin
 * como qualquer outro endpoint de {@code /admin/api/v1/**}, e documenta só esses
 * endpoints (não {@code /api/auth/**} nem {@code /oauth2/**}).
 */
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExigirTokenDePlatformAdminParaApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExporApiDocsComTokenDePlatformAdmin() throws Exception {
        mockMvc.perform(get("/v3/api-docs").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths./admin/api/v1/tenants").exists())
                .andExpect(jsonPath("$.paths./api/auth/login").doesNotExist());
    }
}
