package com.mssousa.authserver.adapter.in.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Corpo de {@code POST /api/auth/consent} (seção 2.2/9 do plano) — decisão do usuário na
 * tela de consentimento Angular, para clients de terceiro
 * ({@code System.thirdParty=true}). {@code scopes} são os escopos que o usuário aprovou
 * (nem sempre todos os solicitados).
 */
public record ConsentRequest(@NotBlank String clientId, @NotEmpty List<@NotBlank String> scopes) {
}
