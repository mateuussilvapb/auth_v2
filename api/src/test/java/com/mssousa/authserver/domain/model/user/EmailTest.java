package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void deveCriarEmailValido() {
        Email email = Email.of("joao@acme.com");
        assertEquals("joao@acme.com", email.value());
    }

    @Test
    void deveNormalizarParaMinusculasETrim() {
        Email email = Email.of("  JOAO@ACME.COM  ");
        assertEquals("joao@acme.com", email.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> Email.of(null));
        assertEquals(Email.DEFAULT_ERROR_EMAIL, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> Email.of(""));
        assertEquals(Email.DEFAULT_ERROR_EMAIL, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaFormatoInvalidoSemArroba() {
        DomainException exception = assertThrows(DomainException.class, () -> Email.of("joao.acme.com"));
        assertEquals(Email.DEFAULT_ERROR_EMAIL_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaFormatoInvalidoSemDominio() {
        DomainException exception = assertThrows(DomainException.class, () -> Email.of("joao@acme"));
        assertEquals(Email.DEFAULT_ERROR_EMAIL_FORMAT, exception.getMessage());
    }

    @Test
    void deveSerIgualPorValorNormalizado() {
        assertEquals(Email.of("joao@acme.com"), Email.of("JOAO@ACME.COM"));
    }

    @Test
    void deveTerToStringIgualAoValor() {
        assertEquals("joao@acme.com", Email.of("joao@acme.com").toString());
    }
}
