package com.mssousa.authserver.config.security.jackson;

import com.mssousa.authserver.application.model.AuthenticatedPlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code AuthenticatedPlatformAdmin} é (des)serializado por este {@code JsonMapper} toda
 * vez que {@code OAuth2Authorization.attributes} é persistido/relido entre
 * {@code /oauth2/authorize} e {@code /oauth2/token} (seção 7.3 do plano) — é o único
 * caminho, fora de um fluxo HTTP completo, que exercita exatamente a mesma
 * (des)serialização usada em produção.
 */
class OAuth2AuthorizationJsonMapperFactoryTest {

    @Test
    void deveSerializarEDesserializarAuthenticatedPlatformAdminPreservandoNomeDeExibicao() {
        JsonMapper mapper = OAuth2AuthorizationJsonMapperFactory.build();
        AuthenticatedPlatformAdmin admin = new AuthenticatedPlatformAdmin(
                PlatformAdminId.of(1L), Username.of("root_admin"), Email.of("admin@seudominio.com"), "Administrador", true);

        String json = mapper.writeValueAsString(admin);
        AuthenticatedPlatformAdmin roundTripped = mapper.readValue(json, AuthenticatedPlatformAdmin.class);

        // Regressão: getName() (exigido por AuthenticatedPrincipal, retorna só o
        // platformAdminId — ver javadoc da classe) colidia com o antigo record accessor
        // name() na introspecção padrão do Jackson — mesmo com @JsonIgnore num dos dois,
        // o Jackson tratava os dois como a mesma propriedade lógica "name" e a ignorava
        // por completo (campo saía null). Renomeado para displayName().
        assertEquals("Administrador", roundTripped.displayName());
        assertEquals("root_admin", roundTripped.username().value());
        assertEquals("admin@seudominio.com", roundTripped.email().value());
        assertEquals(PlatformAdminId.of(1L), roundTripped.platformAdminId());
        assertEquals(true, roundTripped.mustChangePassword());
    }
}
