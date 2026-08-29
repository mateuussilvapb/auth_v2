package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientSecretTest {

    @Test
    void deveCriarClientSecretAPartirDeTextoPlano() {
        ClientSecret secret = ClientSecret.fromPlainText("segredoForte123");
        assertTrue(secret.matches("segredoForte123"));
    }

    @Test
    void hashDeveTerPrefixoBcryptParaCompatibilidadeComSpringSecurity() {
        ClientSecret secret = ClientSecret.fromPlainText("segredoForte123");
        assertTrue(secret.hashedValue().startsWith("{bcrypt}"));
    }

    @Test
    void hashNaoDeveSerIgualAoTextoPlano() {
        ClientSecret secret = ClientSecret.fromPlainText("segredoForte123");
        assertNotEquals("segredoForte123", secret.hashedValue());
    }

    @Test
    void naoDeveCorresponderASecretErrado() {
        ClientSecret secret = ClientSecret.fromPlainText("segredoForte123");
        assertFalse(secret.matches("outroSecret"));
    }

    @Test
    void deveLancarExcecaoParaTextoPlanoNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientSecret.fromPlainText(null));
        assertEquals(ClientSecret.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaTextoPlanoMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientSecret.fromPlainText("curto"));
        assertEquals(ClientSecret.ERROR_MIN_LENGTH, exception.getMessage());
    }

    @Test
    void deveReconstruirAPartirDeHashComPrefixo() {
        ClientSecret original = ClientSecret.fromPlainText("segredoForte123");
        ClientSecret reconstruido = ClientSecret.fromHash(original.hashedValue());
        assertTrue(reconstruido.matches("segredoForte123"));
    }

    @Test
    void deveLancarExcecaoParaHashNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> ClientSecret.fromHash(null));
        assertEquals(ClientSecret.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void naoDeveExporToStringComSecret() {
        ClientSecret secret = ClientSecret.fromPlainText("segredoForte123");
        assertFalse(secret.toString().contains("segredoForte123"));
    }
}
