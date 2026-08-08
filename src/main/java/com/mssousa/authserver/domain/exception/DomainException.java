package com.mssousa.authserver.domain.exception;

/**
 * Exception base para erros de domínio.
 * Representa violações de regras de negócio ou invariantes do domínio.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
