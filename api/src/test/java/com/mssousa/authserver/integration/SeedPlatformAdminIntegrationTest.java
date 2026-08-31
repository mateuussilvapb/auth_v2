package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o seed inicial do platform admin (seção 10, Fase 10, migration {@code V15}) e o
 * mecanismo de troca de senha obrigatória (seção 10, Fase 10 — "senha temporária forçando
 * troca"): o admin seedado consegue logar, mas {@code MustChangePasswordFilter} bloqueia
 * qualquer outra rota de {@code /admin/api/**} até {@code POST .../me/password} suceder.
 */
@AutoConfigureMockMvc
class SeedPlatformAdminIntegrationTest extends AbstractRepositoryIntegrationTest {

    private static final String TEMP_PASSWORD = "TrocarEssaSenha123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlatformAdminRepository platformAdminRepository;

    @Test
    void deveLogarComAdminSeedadoEIndicarMustChangePasswordNaResposta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"console","usernameOrEmail":"admin","password":"%s"}
                                """.formatted(TEMP_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void deveBloquearAdminApiAteTrocarSenhaEDesbloquearDepois() throws Exception {
        PlatformAdmin seeded = platformAdminRepository.findByUsername(Username.of("admin")).orElseThrow();
        assertTrue(seeded.mustChangePassword(), "seed deveria vir com mustChangePassword=true");
        RequestPostProcessor asSeededAdmin = jwt()
                .jwt(builder -> builder.subject(seeded.getId().value().toString())
                        .claim("platform_admin", true)
                        .expiresAt(Instant.now().plusSeconds(300)))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

        mockMvc.perform(get("/admin/api/v1/tenants").with(asSeededAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Troca de senha obrigatória antes de continuar"));

        mockMvc.perform(post("/admin/api/v1/platform-admins/me/password")
                        .with(asSeededAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"novaSenhaSegura123"}
                                """.formatted(TEMP_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        mockMvc.perform(get("/admin/api/v1/tenants").with(asSeededAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void deveRejeitarTrocaDeSenhaComSenhaAtualIncorreta() throws Exception {
        PlatformAdmin seeded = platformAdminRepository.findByUsername(Username.of("admin")).orElseThrow();
        RequestPostProcessor asSeededAdmin = jwt()
                .jwt(builder -> builder.subject(seeded.getId().value().toString())
                        .claim("platform_admin", true)
                        .expiresAt(Instant.now().plusSeconds(300)))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

        mockMvc.perform(post("/admin/api/v1/platform-admins/me/password")
                        .with(asSeededAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"senhaErrada","newPassword":"novaSenhaSegura123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
