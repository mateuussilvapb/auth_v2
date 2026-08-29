package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.ResetTokenValue;
import com.mssousa.authserver.domain.model.user.UserId;

import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findById(PasswordResetTokenId id);

    /**
     * Busca por hash — o valor em texto plano nunca é persistido (ver {@link ResetTokenValue}).
     */
    Optional<PasswordResetToken> findByValue(ResetTokenValue value);

    Optional<PasswordResetToken> findByUserId(UserId userId);

    void deleteById(PasswordResetTokenId id);
}
