package com.mssousa.authserver.adapter.in.web.auth;

import com.mssousa.authserver.application.model.AuthenticatedPlatformAdmin;
import com.mssousa.authserver.application.model.AuthenticatedUser;

/**
 * Resposta de login bem-sucedido — só o necessário para a SPA Angular exibir o usuário
 * logado. A sessão em si vai no cookie {@code HttpOnly} (seção 7.4), não no corpo. Mesmo
 * formato para usuário de tenant e platform admin (login do console administrativo,
 * seção 2.2/D6) — a SPA não precisa distinguir os dois para só exibir nome/username.
 */
public record LoginResponse(String username, String name, boolean mustChangePassword) {

    public static LoginResponse from(AuthenticatedUser authenticatedUser) {
        return new LoginResponse(authenticatedUser.username().value(), authenticatedUser.name(), false);
    }

    public static LoginResponse from(AuthenticatedPlatformAdmin authenticatedPlatformAdmin) {
        return new LoginResponse(authenticatedPlatformAdmin.username().value(),
                authenticatedPlatformAdmin.displayName(), authenticatedPlatformAdmin.mustChangePassword());
    }
}
