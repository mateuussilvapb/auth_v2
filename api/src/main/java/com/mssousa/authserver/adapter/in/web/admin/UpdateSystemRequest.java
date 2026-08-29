package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateSystemRequest(@NotBlank String name) {
}
