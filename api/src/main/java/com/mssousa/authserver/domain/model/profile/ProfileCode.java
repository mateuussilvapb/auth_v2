package com.mssousa.authserver.domain.model.profile;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando o código de um perfil de sistema (ex: "ADMIN",
 * "FINANCEIRO"). Único por sistema, repetível entre sistemas (seção 3.2 do plano).
 * Normalizado para maiúsculas.
 */
public final class ProfileCode {

    public static final String ERROR_REQUIRED = "Código do perfil não pode ser nulo ou vazio";
    public static final String ERROR_FORMAT = "Código do perfil deve conter apenas letras, números e sublinhado";

    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9_]{2,50}$");

    private final String value;

    private ProfileCode(String value) {
        String normalized = value == null ? null : value.toUpperCase().trim();
        validate(normalized);
        this.value = normalized;
    }

    public static ProfileCode of(String value) {
        return new ProfileCode(value);
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
        ProfileCode that = (ProfileCode) o;
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
