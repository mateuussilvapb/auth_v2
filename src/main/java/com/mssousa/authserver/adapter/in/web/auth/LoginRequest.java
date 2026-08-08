package com.mssousa.authserver.adapter.in.web.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /api/auth/login} (seção 7.1 do plano). O {@code clientId} vem da
 * URL original de {@code /oauth2/authorize} preservada pelo Angular — nunca de escolha do
 * usuário (é assim que o tenant é resolvido, seção 2.2).
 */
public record LoginRequest(
        @NotBlank String clientId,
        @NotBlank String usernameOrEmail,
        @NotBlank String password) {
}
