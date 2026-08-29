package com.mssousa.authserver.domain.model.token.passwordResetToken;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.user.UserId;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Entidade de domínio representando um token de redefinição de senha.
 * <p>
 * Só o hash do token é mantido em memória/persistido (ver {@link ResetTokenValue}); o
 * valor em texto plano existe apenas no instante da criação, para ser enviado por
 * e-mail, e nunca fica acessível depois — mesmo padrão de segredo write-once do VO
 * {@code Password}.
 * </p>
 */
public class PasswordResetToken {

    public static final String ERROR_ID_REQUIRED = "ID é obrigatório para PasswordResetToken";
    public static final String ERROR_VALUE_REQUIRED = "Hash do token é obrigatório para PasswordResetToken";
    public static final String ERROR_USER_ID_REQUIRED = "UserId é obrigatório para PasswordResetToken";
    public static final String ERROR_EXPIRES_AT_REQUIRED = "Data de expiração é obrigatória";
    public static final String ERROR_USED_STATUS_REQUIRED = "Status 'used' não pode ser nulo";
    public static final String ERROR_EXPIRED_TOKEN = "Token de redefinição de senha expirado";
    public static final String ERROR_TOKEN_ALREADY_USED = "Token de redefinição de senha já foi utilizado";
    public static final String ERROR_EXPIRATION_MUST_BE_FUTURE = "Data de expiração inválida para novo token. Deve ser futura.";

    private static final int RAW_TOKEN_BYTES = 32;

    private final PasswordResetTokenId id;
    private final ResetTokenValue value;
    private final UserId userId;
    private final Instant expiresAt;
    private Boolean used;

    private PasswordResetToken(Builder builder) {
        this.id = builder.id;
        this.value = builder.value;
        this.userId = builder.userId;
        this.expiresAt = builder.expiresAt;
        this.used = builder.used;

        validate();
    }

    /**
     * Gera um novo token de redefinição de senha. O valor em texto plano retornado deve
     * ser enviado por e-mail e descartado em seguida — só o hash é mantido no objeto.
     *
     * @param id        identificador gerado pela aplicação (TSID)
     * @param userId    usuário que solicitou a redefinição
     * @param expiresAt expiração — a política de TTL (30 min, seção 7.4) é decidida pelo
     *                  caller, não pela entidade
     * @return o token criado (com o hash) e o valor em texto plano gerado
     */
    public static GeneratedToken create(PasswordResetTokenId id, UserId userId, Instant expiresAt) {
        if (expiresAt == null) {
            throw new DomainException(ERROR_EXPIRES_AT_REQUIRED);
        }
        if (!expiresAt.isAfter(Instant.now())) {
            throw new DomainException(ERROR_EXPIRATION_MUST_BE_FUTURE);
        }

        String rawValue = generateRawToken();
        PasswordResetToken token = new Builder()
                .id(id)
                .value(ResetTokenValue.ofRawToken(rawValue))
                .userId(userId)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        return new GeneratedToken(token, rawValue);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (value == null) {
            throw new DomainException(ERROR_VALUE_REQUIRED);
        }
        if (userId == null) {
            throw new DomainException(ERROR_USER_ID_REQUIRED);
        }
        if (expiresAt == null) {
            throw new DomainException(ERROR_EXPIRES_AT_REQUIRED);
        }
        if (used == null) {
            throw new DomainException(ERROR_USED_STATUS_REQUIRED);
        }
    }

    /**
     * Valida se o token pode ser utilizado, lançando exceção caso contrário.
     */
    public void validateUsable() {
        if (isExpired()) {
            throw new DomainException(ERROR_EXPIRED_TOKEN);
        }
        if (isUsed()) {
            throw new DomainException(ERROR_TOKEN_ALREADY_USED);
        }
    }

    /**
     * Marca o token como utilizado. Deve ser chamado exatamente uma vez.
     */
    public void markAsUsed() {
        validateUsable();
        this.used = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return Boolean.TRUE.equals(used);
    }

    public PasswordResetTokenId getId() {
        return id;
    }

    public ResetTokenValue getValue() {
        return value;
    }

    public UserId getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Par (token, valor em texto plano) retornado apenas na criação — ver {@link #create}.
     */
    public record GeneratedToken(PasswordResetToken token, String rawValue) {
    }

    public static class Builder {
        private PasswordResetTokenId id;
        private ResetTokenValue value;
        private UserId userId;
        private Instant expiresAt;
        private Boolean used;

        public Builder id(PasswordResetTokenId id) {
            this.id = id;
            return this;
        }

        public Builder value(ResetTokenValue value) {
            this.value = value;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder used(Boolean used) {
            this.used = used;
            return this;
        }

        public PasswordResetToken build() {
            if (id == null) {
                throw new DomainException(ERROR_ID_REQUIRED);
            }
            if (value == null) {
                throw new DomainException(ERROR_VALUE_REQUIRED);
            }
            if (userId == null) {
                throw new DomainException(ERROR_USER_ID_REQUIRED);
            }
            if (expiresAt == null) {
                throw new DomainException(ERROR_EXPIRES_AT_REQUIRED);
            }
            if (used == null) {
                throw new DomainException(ERROR_USED_STATUS_REQUIRED);
            }
            return new PasswordResetToken(this);
        }
    }
}
