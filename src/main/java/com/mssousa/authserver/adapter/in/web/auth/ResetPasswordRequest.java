package com.mssousa.authserver.adapter.in.web.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /api/auth/reset-password} — {@code token} é o valor em texto
 * plano recebido por e-mail (seção 7.4 do plano).
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank String newPassword) {
}
