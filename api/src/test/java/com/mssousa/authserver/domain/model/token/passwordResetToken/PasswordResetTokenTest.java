package com.mssousa.authserver.domain.model.token.passwordResetToken;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    private static final Instant FUTURE = Instant.now().plus(30, ChronoUnit.MINUTES);

    @Test
    void deveCriarTokenValidoComValorEmTextoPlanoGerado() {
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE);

        assertNotNull(generated.rawValue());
        assertFalse(generated.rawValue().isBlank());
        assertEquals(PasswordResetTokenId.of(1L), generated.token().getId());
        assertEquals(UserId.of(1L), generated.token().getUserId());
        assertFalse(generated.token().isUsed());
        assertFalse(generated.token().isExpired());
    }

    @Test
    void hashArmazenadoDeveCorresponderAoValorEmTextoPlanoGerado() {
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE);

        ResetTokenValue recomputado = ResetTokenValue.ofRawToken(generated.rawValue());
        assertEquals(generated.token().getValue(), recomputado);
    }

    @Test
    void doisTokensGeradosDevemTerValoresDiferentes() {
        PasswordResetToken.GeneratedToken a = PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE);
        PasswordResetToken.GeneratedToken b = PasswordResetToken.create(PasswordResetTokenId.of(2L), UserId.of(1L), FUTURE);

        assertNotEquals(a.rawValue(), b.rawValue());
        assertNotEquals(a.token().getValue(), b.token().getValue());
    }

    @Test
    void deveLancarExcecaoAoCriarComExpiracaoNula() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), null));
        assertEquals(PasswordResetToken.ERROR_EXPIRES_AT_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoAoCriarComExpiracaoNoPassado() {
        Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
        DomainException exception = assertThrows(DomainException.class,
                () -> PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), past));
        assertEquals(PasswordResetToken.ERROR_EXPIRATION_MUST_BE_FUTURE, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoIdNuloAoReconstruir() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PasswordResetToken.builder()
                        .value(ResetTokenValue.ofRawToken("a".repeat(32)))
                        .userId(UserId.of(1L))
                        .expiresAt(FUTURE)
                        .used(false)
                        .build());
        assertEquals(PasswordResetToken.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveMarcarComoUtilizado() {
        PasswordResetToken token = PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE).token();
        token.markAsUsed();
        assertTrue(token.isUsed());
    }

    @Test
    void deveLancarExcecaoAoValidarTokenJaUtilizado() {
        PasswordResetToken token = PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE).token();
        token.markAsUsed();

        DomainException exception = assertThrows(DomainException.class, token::validateUsable);
        assertEquals(PasswordResetToken.ERROR_TOKEN_ALREADY_USED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoAoMarcarComoUtilizadoDuasVezes() {
        PasswordResetToken token = PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE).token();
        token.markAsUsed();

        DomainException exception = assertThrows(DomainException.class, token::markAsUsed);
        assertEquals(PasswordResetToken.ERROR_TOKEN_ALREADY_USED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoAoValidarTokenExpirado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(PasswordResetTokenId.of(1L))
                .value(ResetTokenValue.ofRawToken("a".repeat(32)))
                .userId(UserId.of(1L))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .used(false)
                .build();

        assertTrue(token.isExpired());
        DomainException exception = assertThrows(DomainException.class, token::validateUsable);
        assertEquals(PasswordResetToken.ERROR_EXPIRED_TOKEN, exception.getMessage());
    }

    @Test
    void naoDeveLancarExcecaoParaTokenValidoENaoUtilizado() {
        PasswordResetToken token = PasswordResetToken.create(PasswordResetTokenId.of(1L), UserId.of(1L), FUTURE).token();
        assertDoesNotThrow(token::validateUsable);
    }
}
