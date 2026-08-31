package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;

public record ChangeOwnPasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
