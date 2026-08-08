package com.mssousa.authserver.domain.model.tenant;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando o código único do tenant.
 * Normalizado para minúsculas; usado na resolução de branding e nas URLs.
 */
public final class TenantCode {

    public static final String ERROR_REQUIRED = "Código do tenant não pode ser nulo ou vazio";
    public static final String ERROR_FORMAT = "Código deve conter apenas letras minúsculas, números e hífen";

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$");

    private final String value;

    private TenantCode(String value) {
        String normalized = value == null ? null : value.toLowerCase().trim();
        validate(normalized);
        this.value = normalized;
    }

    public static TenantCode of(String value) {
        return new TenantCode(value);
    }

    private void validate(String v) {
        if (v == null || v.isBlank()) {
            throw new DomainException(ERROR_REQUIRED);
        }
        if (!PATTERN.matcher(v).matches()) {
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
        TenantCode that = (TenantCode) o;
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
