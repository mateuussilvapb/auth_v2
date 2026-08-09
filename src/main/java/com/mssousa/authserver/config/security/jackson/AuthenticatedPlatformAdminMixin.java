package com.mssousa.authserver.config.security.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;

/**
 * Mixin para reconstruir {@code AuthenticatedPlatformAdmin} a partir do JSON persistido em
 * {@code OAuth2Authorization.attributes} (ver {@link AuthenticatedUserMixin}, mesmo padrão).
 */
abstract class AuthenticatedPlatformAdminMixin {

    @JsonCreator
    AuthenticatedPlatformAdminMixin(
            @JsonProperty("platformAdminId") PlatformAdminId platformAdminId,
            @JsonProperty("username") Username username,
            @JsonProperty("email") Email email,
            @JsonProperty("name") String name) {
    }
}
