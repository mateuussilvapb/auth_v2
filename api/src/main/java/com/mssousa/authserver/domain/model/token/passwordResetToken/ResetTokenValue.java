package com.mssousa.authserver.domain.model.token.passwordResetToken;

import com.mssousa.authserver.domain.exception.DomainException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando o hash SHA-256 de um token de redefinição de senha.
 * <p>
 * O valor em texto plano do token **nunca** é armazenado (seção 4.2 e 7.4 do plano) — só
 * o hash. {@link #ofRawToken(String)} recebe o valor gerado uma única vez (para envio por
 * e-mail) e computa o hash a ser persistido; {@link #ofHash(String)} reconstrói o VO a
 * partir do que já está no banco, usado tanto para carregar quanto para buscar por
 * igualdade de hash ao validar um token recebido do usuário.
 * </p>
 */
public final class ResetTokenValue {

    public static final String ERROR_RAW_TOKEN_REQUIRED = "Token de reset não pode ser nulo ou vazio";
    public static final String ERROR_RAW_TOKEN_MIN_LENGTH = "Token de reset inválido";
    public static final String ERROR_HASH_REQUIRED = "Hash do token de reset não pode ser nulo ou vazio";
    public static final String ERROR_HASH_FORMAT = "Hash do token de reset em formato inválido";

    private static final int RAW_TOKEN_MIN_LENGTH = 32;
    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[a-f0-9]{64}$");

    private final String hash;

    private ResetTokenValue(String hash) {
        this.hash = hash;
    }

    /**
     * Computa o hash SHA-256 de um token em texto plano recém-gerado.
     */
    public static ResetTokenValue ofRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new DomainException(ERROR_RAW_TOKEN_REQUIRED);
        }
        if (rawToken.length() < RAW_TOKEN_MIN_LENGTH) {
            throw new DomainException(ERROR_RAW_TOKEN_MIN_LENGTH);
        }
        return new ResetTokenValue(sha256Hex(rawToken));
    }

    /**
     * Reconstrói o VO a partir de um hash já persistido (ou já calculado, para busca).
     */
    public static ResetTokenValue ofHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new DomainException(ERROR_HASH_REQUIRED);
        }
        if (!SHA256_HEX_PATTERN.matcher(hash).matches()) {
            throw new DomainException(ERROR_HASH_FORMAT);
        }
        return new ResetTokenValue(hash);
    }

    private static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }

    public String value() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResetTokenValue that = (ResetTokenValue) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public String toString() {
        return hash;
    }
}
