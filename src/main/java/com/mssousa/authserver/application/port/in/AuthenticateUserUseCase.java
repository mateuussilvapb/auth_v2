package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedUser;

/**
 * Porta de entrada para autenticação de usuário (seção 7.1: {@code POST /api/auth/login}).
 * O tenant é sempre resolvido a partir do {@code client_id} — nunca de input do usuário.
 */
public interface AuthenticateUserUseCase {

    /**
     * @throws AuthenticationFailedException com mensagem sempre genérica (seção 6.6),
     *                                        qualquer que seja o motivo da falha
     */
    AuthenticatedUser authenticate(String clientId, String usernameOrEmail, String plainPassword);
}
