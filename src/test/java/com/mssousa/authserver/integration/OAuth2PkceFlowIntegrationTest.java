package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o fluxo Authorization Code + PKCE de ponta a ponta (seção 7.1) contra os
 * endpoints reais {@code POST /api/auth/login}, {@code /oauth2/authorize} e
 * {@code /oauth2/token}, confirmando emissão e validação de token (assinatura + claims da
 * seção 7.2) a partir de um login real (Fase 7), sessão real e tudo.
 */
@AutoConfigureMockMvc
class OAuth2PkceFlowIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private SystemTenantRepository systemTenantRepository;
    @Autowired
    private UserSystemRepository userSystemRepository;

    @Test
    void deveEmitirTokenComClaimsCorretosViaAuthorizationCodeComPkce() throws Exception {
        Tenant tenant = createAndSaveTenant("acme");
        System system = createAndSaveSystem("CRM_ACME");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "joao_silva", "joao@acme.com");
        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());

        String redirectUri = system.getRedirectUris().get(0).value();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s","usernameOrEmail":"joao_silva","password":"senhaSegura123"}
                                """.formatted(system.getClientId().value())))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "login deveria ter criado uma sessão");

        MvcResult authorizeResult = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", system.getClientId().value())
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
                        .param("client_id", system.getClientId().value())
                        .param("code_verifier", codeVerifier)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = new ObjectMapper().readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = body.get("access_token").asText();
        assertNotNull(accessToken);

        Jwt jwt = jwtDecoder.decode(accessToken);
        assertEquals(tenant.getId().value().toString(), jwt.getClaimAsString("tenant_id"));
        assertEquals("acme", jwt.getClaimAsString("tenant_code"));
        assertEquals(system.getClientId().value(), jwt.getClaimAsString("client_id"));
        assertEquals("joao_silva", jwt.getClaimAsString("username"));
        assertEquals("joao@acme.com", jwt.getClaimAsString("email"));
        assertEquals(user.getId().value().toString(), jwt.getSubject());
    }

    @Test
    void deveRejeitarAuthorizeComCodeChallengeMethodPlain() throws Exception {
        Tenant tenant = createAndSaveTenant("globex");
        System system = createAndSaveSystem("CRM_GLOBEX");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                com.mssousa.authserver.domain.model.user.UserId.of(idGenerator.generate()), tenant.getId(), system.getId(),
                com.mssousa.authserver.domain.model.user.Username.of("qualquer"),
                com.mssousa.authserver.domain.model.user.Email.of("qualquer@globex.com"), "Qualquer");
        Authentication principal = ClientAwareAuthenticationToken.authenticated(
                system.getClientId().value(), authenticatedUser, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        MvcResult result = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", system.getClientId().value())
                        .queryParam("redirect_uri", system.getRedirectUris().get(0).value())
                        .queryParam("code_challenge", "qualquer-coisa")
                        .queryParam("code_challenge_method", "plain")
                        .queryParam("state", "xyz")
                        .with(authentication(principal)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertNotNull(location);
        String error = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("error");
        assertEquals("invalid_request", error, "esperava erro invalid_request no redirect: " + location);
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
