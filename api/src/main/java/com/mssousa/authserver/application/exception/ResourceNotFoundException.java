package com.mssousa.authserver.application.exception;

/**
 * Lançada quando um recurso solicitado não existe. Distinta de {@code DomainException}
 * (violação de invariante, seção 6.6 do plano) — mapeada para 404, não 422.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
