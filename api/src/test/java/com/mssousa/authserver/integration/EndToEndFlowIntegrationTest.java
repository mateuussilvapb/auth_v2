package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.application.port.out.EmailSenderPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de ponta a ponta (seção 10, Fase 10, e roteiro manual da seção 14 do plano):
 * platform admin cria tenant → sistema → perfil → usuário → vincula usuário ao sistema e
 * ao perfil via {@code /admin/api/v1/**} (a mesma API que o console Angular consome, não
 * repositórios diretos), autentica o usuário, completa o Authorization Code + PKCE e
 * valida os claims do JWT emitido — reproduz automatizado o que o roteiro manual da
 * seção 14 (passos 3–10) e o `PROGRESS.md` (nota "Roteiro E2E manual completo", Fase 8)
 * já validaram uma vez à mão.
 */
@AutoConfigureMockMvc
class EndToEndFlowIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private EmailSenderPort emailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveCompletarFluxoDeTenantAteLoginComClaimsCorretos() throws Exception {
        // 1. Tenant
        JsonNode tenantResponse = readBody(mockMvc.perform(post("/admin/api/v1/tenants")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"e2e-flow","name":"E2E Flow Corp"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        long tenantId = tenantResponse.get("id").asLong();

        // 2. Sistema
        JsonNode systemResponse = readBody(mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"E2E_FLOW_SYS","name":"E2E Flow Sys","publicClient":true,
                                 "initialRedirectUris":["https://e2e-flow.example.com/callback"],"thirdParty":false}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        long systemId = systemResponse.get("id").asLong();
        String clientId = systemResponse.get("clientId").asText();
        String redirectUri = systemResponse.get("redirectUris").get(0).asText();

        // 3. Perfil
        JsonNode profileResponse = readBody(mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        long profileId = profileResponse.get("id").asLong();

        // 4. Usuário
        JsonNode userResponse = readBody(mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"e2e_user","email":"e2e_user@e2e-flow.com",
                                 "password":"senhaSegura123","name":"E2E User"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        long userId = userResponse.get("id").asLong();

        // 5. Vínculo usuário↔sistema
        JsonNode userSystemResponse = readBody(mockMvc.perform(
                        post("/admin/api/v1/tenants/" + tenantId + "/users/" + userId + "/systems")
                                .with(platformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"systemId\":" + systemId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn());
        long userSystemId = userSystemResponse.get("id").asLong();

        // 6. Vínculo usuário↔perfil
        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":" + profileId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 7. Login (sessão) — POST /api/auth/login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"e2e_user","password":"senhaSegura123"}
                                """.formatted(clientId)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "login deveria ter criado uma sessão");

        // 8. Authorization Code + PKCE
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        MvcResult authorizeResult = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", clientId)
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .queryParam("state", "xyz")
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
                        .param("client_id", clientId)
                        .param("code_verifier", codeVerifier)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokenBody = readBody(tokenResult);
        String accessToken = tokenBody.get("access_token").asText();
        assertNotNull(accessToken);

        // 9. Validar claims (assinatura validada implicitamente — jwtDecoder.decode()
        // usa o mesmo JWKSource exposto em /oauth2/jwks; falha se a assinatura não bater).
        Jwt jwt = jwtDecoder.decode(accessToken);
        assertEquals(String.valueOf(tenantId), jwt.getClaimAsString("tenant_id"));
        assertEquals("e2e-flow", jwt.getClaimAsString("tenant_code"));
        assertEquals(clientId, jwt.getClaimAsString("client_id"));
        assertEquals("e2e_user", jwt.getClaimAsString("username"));
        assertEquals("e2e_user@e2e-flow.com", jwt.getClaimAsString("email"));
        assertEquals(String.valueOf(userId), jwt.getSubject());
        assertEquals(java.util.List.of("ADMIN"), jwt.getClaimAsStringList("profiles"));

        mockMvc.perform(get("/oauth2/jwks")).andExpect(status().isOk());
    }

    @Test
    void deveRejeitarVinculoDeUsuarioASistemaDeOutroTenant() throws Exception {
        long tenantAId = createTenantAndReturnId("e2e-tenant-a");
        long tenantBId = createTenantAndReturnId("e2e-tenant-b");
        long systemBId = createSystemAndReturnId(tenantBId, "E2E_TENANT_B_SYS");

        JsonNode userResponse = readBody(mockMvc.perform(post("/admin/api/v1/tenants/" + tenantAId + "/users")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"e2e_cross_tenant","email":"cross@e2e-tenant-a.com",
                                 "password":"senhaSegura123","name":"Cross Tenant"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        long userId = userResponse.get("id").asLong();

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantAId + "/users/" + userId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":" + systemBId + "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRejeitarLoginQuandoTenantEstaDesativado() throws Exception {
        long tenantId = createTenantAndReturnId("e2e-tenant-inactive");
        JsonNode systemResponse = readBody(mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"E2E_INACTIVE_SYS","name":"E2E Inactive Sys","publicClient":true,
                                 "initialRedirectUris":["https://e2e-inactive.example.com/callback"],"thirdParty":false}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        String clientId = systemResponse.get("clientId").asText();

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"e2e_inactive_user","email":"inactive@e2e-tenant-inactive.com",
                                 "password":"senhaSegura123","name":"Inactive User"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/admin/api/v1/tenants/" + tenantId + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"e2e_inactive_user","password":"senhaSegura123"}
                                """.formatted(clientId)))
                .andExpect(status().isUnauthorized());
    }

    private long createTenantAndReturnId(String code) throws Exception {
        JsonNode response = readBody(mockMvc.perform(post("/admin/api/v1/tenants")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andReturn());
        return response.get("id").asLong();
    }

    private long createSystemAndReturnId(long tenantId, String clientId) throws Exception {
        JsonNode response = readBody(mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + clientId + "\",\"name\":\"" + clientId + "\",\"publicClient\":true,"
                                + "\"initialRedirectUris\":[\"https://" + clientId.toLowerCase() + ".example.com/callback\"],"
                                + "\"thirdParty\":false}"))
                .andExpect(status().isCreated())
                .andReturn());
        return response.get("id").asLong();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
