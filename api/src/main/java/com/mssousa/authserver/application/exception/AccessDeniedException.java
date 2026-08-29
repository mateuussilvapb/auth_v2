package com.mssousa.authserver.application.exception;

/**
 * Lançada quando um usuário já autenticado tenta acessar algo fora do seu escopo (ex:
 * autorizar um sistema ao qual não está vinculado). Distinta de
 * {@code AuthenticationFailedException} — mapeada para 403, não 401.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
