package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando um endereço de e-mail.
 * Único por tenant (não globalmente) — ver decisão D2 do plano.
 */
public final class Email {

    public static final String DEFAULT_ERROR_EMAIL = "Email não pode ser nulo ou vazio";
    public static final String DEFAULT_ERROR_EMAIL_FORMAT = "Formato de email inválido";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        String normalized = normalize(value);
        validate(normalized);
        this.value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(DEFAULT_ERROR_EMAIL);
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new DomainException(DEFAULT_ERROR_EMAIL_FORMAT);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase().trim();
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
