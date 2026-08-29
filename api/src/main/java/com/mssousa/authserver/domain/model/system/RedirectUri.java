package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.Objects;

/**
 * Value Object representando uma URI de redirecionamento válida para o fluxo
 * Authorization Code + PKCE. Um sistema pode ter múltiplas (dev, homolog, prod).
 */
public final class RedirectUri {

    public static final String ERROR_REQUIRED = "Redirect URI não pode ser nula ou vazia";
    public static final String ERROR_SCHEME = "Redirect URI deve começar com http:// ou https://";
    public static final String ERROR_MAX_LENGTH = "Redirect URI não pode exceder 500 caracteres";

    private static final int MAX_LENGTH = 500;

    private final String value;

    private RedirectUri(String value) {
        validate(value);
        this.value = value;
    }

    public static RedirectUri of(String value) {
        return new RedirectUri(value);
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(ERROR_REQUIRED);
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new DomainException(ERROR_SCHEME);
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainException(ERROR_MAX_LENGTH);
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedirectUri that = (RedirectUri) o;
        return value.equals(that.value);
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
