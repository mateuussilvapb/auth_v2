package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;

/**
 * Value Object representando o secret de um client confidencial, sempre em hash BCrypt
 * (seção 7.3 do plano — nunca texto plano). O prefixo {@code {bcrypt}} é mantido no hash
 * armazenado de propósito: é o formato que o {@code DelegatingPasswordEncoder} do Spring
 * Security espera ao validar o secret durante a autenticação do client OAuth2, evitando
 * qualquer configuração adicional para os dois lados concordarem.
 */
public final class ClientSecret {

    public static final String ERROR_REQUIRED = "Client Secret não pode ser nulo ou vazio";
    public static final String ERROR_MIN_LENGTH = "Client Secret deve ter pelo menos 8 caracteres";

    private static final String PREFIX = "{bcrypt}";
    private static final int MIN_LENGTH = 8;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private final String hashedValue;

    private ClientSecret(String hashedValue) {
        this.hashedValue = hashedValue;
    }

    public static ClientSecret fromPlainText(String plainSecret) {
        if (plainSecret == null || plainSecret.isBlank()) {
            throw new DomainException(ERROR_REQUIRED);
        }
        if (plainSecret.length() < MIN_LENGTH) {
            throw new DomainException(ERROR_MIN_LENGTH);
        }
        return new ClientSecret(PREFIX + ENCODER.encode(plainSecret));
    }

    public static ClientSecret fromHash(String hashedValue) {
        if (hashedValue == null || hashedValue.isBlank()) {
            throw new DomainException(ERROR_REQUIRED);
        }
        return new ClientSecret(hashedValue);
    }

    public boolean matches(String plainSecret) {
        if (plainSecret == null) {
            return false;
        }
        return ENCODER.matches(plainSecret, stripPrefix(hashedValue));
    }

    /**
     * Retorna o hash com o prefixo {@code {bcrypt}}, pronto para persistência e para
     * {@code RegisteredClient.clientSecret(...)}.
     */
    public String hashedValue() {
        return hashedValue;
    }

    private String stripPrefix(String value) {
        return value.startsWith(PREFIX) ? value.substring(PREFIX.length()) : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientSecret that = (ClientSecret) o;
        return hashedValue.equals(that.hashedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashedValue);
    }

    // Não implementar toString() por questões de segurança
}
