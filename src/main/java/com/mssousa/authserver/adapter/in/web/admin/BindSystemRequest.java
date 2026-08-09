package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

/** {@code systemId} é {@code String} — ver javadoc de {@link TenantResponse}. */
public record BindSystemRequest(@NotBlank String systemId) {
}
