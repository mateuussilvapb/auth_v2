package com.mssousa.authserver.application.exception;

/**
 * Lançada quando a autenticação falha, por qualquer motivo (usuário inexistente, senha
 * incorreta, tenant/sistema/vínculo inativos). Mensagem sempre genérica — nunca revela
 * qual etapa falhou (seção 6.6 do plano). Mapeada para 401.
 */
public class AuthenticationFailedException extends RuntimeException {

    public static final String GENERIC_MESSAGE = "Invalid credentials";

    public AuthenticationFailedException() {
        super(GENERIC_MESSAGE);
    }
}
