package com.mssousa.authserver.config.security.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;

/**
 * Mixin para reconstruir {@code AuthenticatedUser} a partir do JSON persistido em
 * {@code OAuth2Authorization.attributes} (ver {@link ValueObjectMixins}).
 */
abstract class AuthenticatedUserMixin {

    @JsonCreator
    AuthenticatedUserMixin(
            @JsonProperty("userId") UserId userId,
            @JsonProperty("tenantId") TenantId tenantId,
            @JsonProperty("systemId") SystemId systemId,
            @JsonProperty("username") Username username,
            @JsonProperty("email") Email email,
            @JsonProperty("name") String name) {
    }
}
