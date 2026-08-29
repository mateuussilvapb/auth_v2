package com.mssousa.authserver.config.security.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Mixin para reconstruir {@link com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken}
 * a partir do JSON persistido pelo {@code JdbcOAuth2AuthorizationService} — só o estado
 * autenticado é serializado (o estado não-autenticado nunca chega a ser persistido; a
 * autenticação acontece antes do fluxo OAuth2 propriamente dito, seção 2.2 do plano).
 * Getters derivados ({@code authenticated}, {@code details}, {@code name}, {@code credentials})
 * são ignorados: não fazem parte do construtor de reconstrução e são sempre recalculáveis.
 */
@JsonIgnoreProperties(ignoreUnknown = true, value = {"authenticated", "details", "name", "credentials"})
abstract class ClientAwareAuthenticationTokenMixin {

    @JsonCreator
    static Authentication authenticated(
            @JsonProperty("clientId") String clientId,
            @JsonProperty("principal") AuthenticatedUser authenticatedUser,
            @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
        return null;
    }
}
