package com.mssousa.authserver.adapter.in.web.auth;

import com.mssousa.authserver.application.model.AuthenticatedUser;

/**
 * Resposta de login bem-sucedido — só o necessário para a SPA Angular exibir o usuário
 * logado. A sessão em si vai no cookie {@code HttpOnly} (seção 7.4), não no corpo.
 */
public record LoginResponse(String username, String name) {

    public static LoginResponse from(AuthenticatedUser authenticatedUser) {
        return new LoginResponse(authenticatedUser.username().value(), authenticatedUser.name());
    }
}
