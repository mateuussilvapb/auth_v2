package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

/** {@code profileId} é {@code String} — ver javadoc de {@link TenantResponse}. */
public record BindProfileRequest(@NotBlank String profileId) {
}
