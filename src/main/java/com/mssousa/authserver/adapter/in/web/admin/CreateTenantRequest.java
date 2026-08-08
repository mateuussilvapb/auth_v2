package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(@NotBlank String code, @NotBlank String name) {
}
