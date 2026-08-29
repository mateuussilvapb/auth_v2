package com.mssousa.authserver.domain.model.tenant;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantCodeTest {

    @Test
    void deveCriarCodigoValido() {
        TenantCode code = TenantCode.of("acme");
        assertEquals("acme", code.value());
    }

    @Test
    void deveCriarCodigoComHifen() {
        TenantCode code = TenantCode.of("acme-corp");
        assertEquals("acme-corp", code.value());
    }

    @Test
    void deveNormalizarParaMinusculasETrim() {
        TenantCode code = TenantCode.of("  ACME  ");
        assertEquals("acme", code.value());
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of(null));
        assertEquals(TenantCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorVazio() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of(""));
        assertEquals(TenantCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaValorEmBranco() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of("   "));
        assertEquals(TenantCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaCaractereInvalido() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of("acme_corp"));
        assertEquals(TenantCode.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaCodigoMuitoCurto() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of("a"));
        assertEquals(TenantCode.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaComecarComHifen() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of("-acme"));
        assertEquals(TenantCode.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoParaTerminarComHifen() {
        DomainException exception = assertThrows(DomainException.class, () -> TenantCode.of("acme-"));
        assertEquals(TenantCode.ERROR_FORMAT, exception.getMessage());
    }

    @Test
    void deveSerIgualPorValor() {
        assertEquals(TenantCode.of("acme"), TenantCode.of("ACME"));
        assertNotEquals(TenantCode.of("acme"), TenantCode.of("globex"));
    }

    @Test
    void deveTerToStringIgualAoValor() {
        assertEquals("acme", TenantCode.of("acme").toString());
    }
}
