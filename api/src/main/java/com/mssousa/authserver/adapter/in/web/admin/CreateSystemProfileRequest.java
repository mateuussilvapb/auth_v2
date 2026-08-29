package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateSystemProfileRequest(@NotBlank String code, String description) {
}
