package com.mssousa.authserver.domain.model.token.passwordResetToken;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um token de redefinição de senha.
 */
public final class PasswordResetTokenId extends DomainId {

    private PasswordResetTokenId(Long value) {
        super(value);
        validate(value);
    }

    public static PasswordResetTokenId of(Long value) {
        return new PasswordResetTokenId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("PasswordResetTokenId deve ser um número positivo");
        }
    }
}
