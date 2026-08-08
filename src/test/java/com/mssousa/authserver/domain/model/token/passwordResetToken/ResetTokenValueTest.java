package com.mssousa.authserver.domain.model.token.passwordResetToken;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResetTokenValueTest {

    private static final String RAW_TOKEN = "a".repeat(32);

    @Test
    void deveComputarHashDeterministicoParaOMesmoTokenBruto() {
        ResetTokenValue a = ResetTokenValue.ofRawToken(RAW_TOKEN);
        ResetTokenValue b = ResetTokenValue.ofRawToken(RAW_TOKEN);

        assertEquals(a, b);
        assertEquals(a.value(), b.value());
    }

    @Test
    void hashNaoDeveSerIgualAoTokenBruto() {
        ResetTokenValue value = ResetTokenValue.ofRawToken(RAW_TOKEN);
        assertNotEquals(RAW_TOKEN, value.value());
    }

    @Test
    void hashDeveTer64CaracteresHexadecimais() {
        ResetTokenValue value = ResetTokenValue.ofRawToken(RAW_TOKEN);
        assertEquals(64, value.value().length());
        assertTrue(value.value().matches("^[a-f0-9]{64}$"));
    }

    @Test
    void tokensDiferentesDevemGerarHashesDiferentes() {
        ResetTokenValue a = ResetTokenValue.ofRawToken(RAW_TOKEN);
        ResetTokenValue b = ResetTokenValue.ofRawToken("b".repeat(32));
        assertNotEquals(a, b);
    }

    @Test
    void deveLancarExcecaoParaTokenBrutoNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> ResetTokenValue.ofRawToken(null));
        assertEquals(ResetTokenValue.ERROR_RAW_TOKEN_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaTokenBrutoVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> ResetTokenValue.ofRawToken(""));
        assertEquals(ResetTokenValue.ERROR_RAW_TOKEN_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaTokenBrutoMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> ResetTokenValue.ofRawToken("curto"));
        assertEquals(ResetTokenValue.ERROR_RAW_TOKEN_MIN_LENGTH, exception.getMessage());
    }

    @Test
    void deveReconstruirAPartirDeHashValido() {
        ResetTokenValue original = ResetTokenValue.ofRawToken(RAW_TOKEN);
        ResetTokenValue reconstruido = ResetTokenValue.ofHash(original.value());

        assertEquals(original, reconstruido);
    }

    @Test
    void deveLancarExcecaoParaHashNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> ResetTokenValue.ofHash(null));
        assertEquals(ResetTokenValue.ERROR_HASH_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaHashComFormatoInvalido() {
        DomainException exception = assertThrows(DomainException.class, () -> ResetTokenValue.ofHash("nao-e-um-hash-sha256"));
        assertEquals(ResetTokenValue.ERROR_HASH_FORMAT, exception.getMessage());
    }
}
