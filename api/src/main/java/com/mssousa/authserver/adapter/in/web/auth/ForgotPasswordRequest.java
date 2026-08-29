package com.mssousa.authserver.adapter.in.web.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /api/auth/forgot-password} (seção 7.4 do plano). Mesma regra do
 * login: {@code clientId} vem da URL original, nunca de escolha do usuário.
 */
public record ForgotPasswordRequest(
        @NotBlank String clientId,
        @NotBlank String usernameOrEmail) {
}
