package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o fluxo completo de consentimento (seção 2.2/9 do plano) para clients de
 * terceiro ({@code System.thirdParty=true}): login real -> {@code GET /oauth2/authorize}
 * redireciona para {@code /consent} (ainda sem consentimento gravado) ->
 * {@code POST /api/auth/consent} -> {@code GET /oauth2/authorize} de novo (agora sucede)
 * -> troca de código por token.
 */
@AutoConfigureMockMvc
class OAuth2ConsentFlowIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private SystemRepository systemRepository;
    @Autowired
    private SystemTenantRepository systemTenantRepository;
    @Autowired
    private UserSystemRepository userSystemRepository;

    @Test
    void devePedirConsentimentoParaClientDeTerceiroEEmitirTokenAposAprovacao() throws Exception {
        Tenant tenant = createAndSaveTenant("parceiros");
        System thirdPartySystem = System.builder()
                .id(SystemId.of(idGenerator.generate()))
                .clientId(ClientId.of("PARCEIRO_EXTERNO"))
                .name("Parceiro Externo")
                .thirdParty(true)
                .redirectUri(RedirectUri.of("https://parceiro.example.com/callback"))
                .build();
        systemRepository.save(thirdPartySystem);
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(thirdPartySystem.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "joao_parceiro", "joao@parceiros.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(thirdPartySystem.getId()).tenantId(tenant.getId()).build());

        String redirectUri = thirdPartySystem.getRedirectUris().get(0).value();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"joao_parceiro","password":"senhaSegura123"}
                                """.formatted(thirdPartySystem.getClientId().value())))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        MvcResult firstAuthorize = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", thirdPartySystem.getClientId().value())
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("scope", "profile")
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .queryParam("state", "xyz")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String consentRedirect = firstAuthorize.getResponse().getRedirectedUrl();
        assertNotNull(consentRedirect);
        var consentParams = UriComponentsBuilder.fromUriString(consentRedirect).build().getQueryParams();
        assertEquals("PARCEIRO_EXTERNO", consentParams.getFirst("client_id"));

        mockMvc.perform(post("/api/auth/consent")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"PARCEIRO_EXTERNO","scopes":["profile"]}
                                """))
                .andExpect(status().isOk());

        MvcResult secondAuthorize = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", thirdPartySystem.getClientId().value())
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("scope", "profile")
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .queryParam("state", "xyz")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = secondAuthorize.getResponse().getRedirectedUrl();
        assertNotNull(location);
        String code = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");
        assertNotNull(code, "esperava um ?code= após o consentimento: " + location);

        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", redirectUri)
                        .param("client_id", thirdPartySystem.getClientId().value())
                        .param("code_verifier", codeVerifier)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = new ObjectMapper().readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = body.get("access_token").asText();
        Jwt jwt = jwtDecoder.decode(accessToken);
        assertEquals("joao_parceiro", jwt.getClaimAsString("username"));
    }

    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
