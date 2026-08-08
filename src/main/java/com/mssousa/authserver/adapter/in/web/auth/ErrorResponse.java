package com.mssousa.authserver.adapter.in.web.auth;

/**
 * Corpo de erro genérico da API pública de autenticação (seção 7.4: nunca vazar qual
 * etapa falhou — client_id inválido, usuário inexistente, senha errada e conta bloqueada
 * retornam exatamente a mesma mensagem).
 */
public record ErrorResponse(String message) {
}
