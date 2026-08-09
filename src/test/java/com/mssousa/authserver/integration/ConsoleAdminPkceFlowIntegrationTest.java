package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo Authorization Code + PKCE do console administrativo Angular (seção 2.2/D6 do
 * plano, Fase 9) — o client estático de {@code RegisteredClientRepositoryConfig}, não um
 * {@code System} da tabela {@code system}. O login usa o mesmo
 * {@code POST /api/auth/login} dos usuários de tenant, mas cai no fallback do
 * {@code ProviderManager} para {@code PlatformAdminAuthenticationProvider} (client_id
 * "console" não resolve nenhum {@code System}), e o token emitido carrega
 * {@code platform_admin: true} em vez dos claims de tenant/perfis.
 */
@AutoConfigureMockMvc
class ConsoleAdminPkceFlowIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private PlatformAdminRepository platformAdminRepository;
    @Value("${authserver.console-client.client-id}")
    private String consoleClientId;
    @Value("${authserver.console-client.redirect-uris}")
    private String consoleRedirectUris;

    private PlatformAdmin createAndSaveAdmin(String username, String email) {
        PlatformAdmin admin = PlatformAdmin.builder()
                .id(PlatformAdminId.of(idGenerator.generate()))
                .username(Username.of(username))
                .email(Email.of(email))
                .password(Password.fromPlainText("senhaSegura123"))
                .name(username)
                .build();
        return platformAdminRepository.save(admin);
    }

    @Test
    void deveEmitirTokenComClaimPlatformAdminViaConsoleAdministrativo() throws Exception {
        createAndSaveAdmin("root_admin", "admin@seudominio.com");

        String redirectUri = consoleRedirectUris.split(",")[0].trim();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"root_admin","password":"senhaSegura123"}
                                """.formatted(consoleClientId)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "login deveria ter criado uma sessão");

        MvcResult authorizeResult = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", consoleClientId)
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .queryParam("state", "xyz")
                        .queryParam("scope", "profile")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = authorizeResult.getResponse().getRedirectedUrl();
        assertNotNull(location);
        String code = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");
        assertNotNull(code, "esperava um ?code= no redirect: " + location);

        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", redirectUri)
                        .param("client_id", consoleClientId)
                        .param("code_verifier", codeVerifier)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = new ObjectMapper().readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = body.get("access_token").asText();
        assertNotNull(accessToken);

        Jwt jwt = jwtDecoder.decode(accessToken);
        assertEquals(Boolean.TRUE, jwt.getClaimAsBoolean("platform_admin"));
        assertEquals("root_admin", jwt.getClaimAsString("username"));
        assertEquals("admin@seudominio.com", jwt.getClaimAsString("email"));
        assertNull(jwt.getClaimAsString("tenant_id"), "token de platform admin não deveria carregar tenant_id");
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
