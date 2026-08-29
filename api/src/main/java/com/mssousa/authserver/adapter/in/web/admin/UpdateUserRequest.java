package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank String name, @NotBlank String email) {
}
