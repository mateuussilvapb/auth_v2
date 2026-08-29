package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando o client_id OAuth2 de um sistema satélite.
 * Único e imutável globalmente — é a chave usada para resolver o tenant (seção 7.1).
 */
public final class ClientId {

    public static final String ERROR_REQUIRED = "Client ID não pode ser nulo ou vazio";
    public static final String ERROR_FORMAT = "Client ID deve conter apenas letras maiúsculas, números, hífen e sublinhado";

    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{1,98}[A-Z0-9]$");

    private final String value;

    private ClientId(String value) {
        validate(value);
        this.value = value;
    }

    public static ClientId of(String value) {
        return new ClientId(value);
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(ERROR_REQUIRED);
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException(ERROR_FORMAT);
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientId clientId = (ClientId) o;
        return value.equals(clientId.value);
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
