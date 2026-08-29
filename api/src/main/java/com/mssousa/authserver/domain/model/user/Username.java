package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando o nome de usuário.
 * Único por tenant (não globalmente) — ver decisão D2 do plano.
 */
public final class Username {

    public static final String DEFAULT_ERROR_USERNAME = "Username não pode ser nulo ou vazio";
    public static final String DEFAULT_ERROR_USERNAME_MIN_LENGTH = "Username deve ter pelo menos 3 caracteres";
    public static final String DEFAULT_ERROR_USERNAME_MAX_LENGTH = "Username não pode exceder 50 caracteres";
    public static final String DEFAULT_ERROR_USERNAME_PATTERN = "Username deve conter apenas caracteres alfanuméricos e sublinhados";

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final String value;

    private Username(String value) {
        validate(value);
        this.value = value;
    }

    public static Username of(String value) {
        return new Username(value);
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(DEFAULT_ERROR_USERNAME);
        }

        if (value.length() < MIN_LENGTH) {
            throw new DomainException(DEFAULT_ERROR_USERNAME_MIN_LENGTH);
        }

        if (value.length() > MAX_LENGTH) {
            throw new DomainException(DEFAULT_ERROR_USERNAME_MAX_LENGTH);
        }

        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new DomainException(DEFAULT_ERROR_USERNAME_PATTERN);
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Username username = (Username) o;
        return value.equals(username.value);
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
