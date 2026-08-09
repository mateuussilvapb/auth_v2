package com.mssousa.authserver.integration;

import com.mssousa.authserver.domain.model.system.System;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa que o redirecionamento não autenticado para {@code /login} (seção 2.2 do plano)
 * preserva a query string original de {@code GET /oauth2/authorize} — sem isso, a SPA
 * Angular não tem como resolver o branding do tenant nem retomar o fluxo depois do login.
 */
@AutoConfigureMockMvc
class SpaLoginAuthenticationEntryPointIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRedirecionarParaLoginPreservandoQueryStringOriginal() throws Exception {
        System system = createAndSaveSystem("CRM_ACME_ENTRYPOINT");
        String redirectUri = system.getRedirectUris().get(0).value();

        mockMvc.perform(get("/oauth2/authorize")
                        .accept(MediaType.TEXT_HTML)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", system.getClientId().value())
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("code_challenge", "abc123")
                        .queryParam("code_challenge_method", "S256")
                        .queryParam("state", "xyz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("/login?"),
                        org.hamcrest.Matchers.containsString("client_id=" + system.getClientId().value()),
                        org.hamcrest.Matchers.containsString("state=xyz"))));
    }
}
