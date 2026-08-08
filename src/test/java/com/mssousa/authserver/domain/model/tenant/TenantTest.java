package com.mssousa.authserver.domain.model.tenant;

import com.mssousa.authserver.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantTest {

    private Tenant.Builder validBuilder() {
        return Tenant.builder()
                .id(TenantId.of(1L))
                .code(TenantCode.of("acme"))
                .name("Acme Corp");
    }

    @Test
    void deveCriarTenantValidoComStatusPadraoActive() {
        Tenant tenant = validBuilder().build();

        assertEquals(TenantId.of(1L), tenant.getId());
        assertEquals(TenantCode.of("acme"), tenant.getCode());
        assertEquals("Acme Corp", tenant.getName());
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        assertTrue(tenant.isActive());
    }

    @Test
    void deveCriarTenantComStatusExplicito() {
        Tenant tenant = validBuilder().status(TenantStatus.INACTIVE).build();
        assertEquals(TenantStatus.INACTIVE, tenant.getStatus());
        assertFalse(tenant.isActive());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> Tenant.builder().code(TenantCode.of("acme")).name("Acme").build());
        assertEquals(Tenant.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoCodeNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> Tenant.builder().id(TenantId.of(1L)).name("Acme").build());
        assertEquals(Tenant.ERROR_CODE_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNomeNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).build());
        assertEquals(Tenant.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNomeEmBranco() {
        DomainException exception = assertThrows(DomainException.class,
                () -> validBuilder().name("   ").build());
        assertEquals(Tenant.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtivarTenant() {
        Tenant tenant = validBuilder().status(TenantStatus.INACTIVE).build();
        tenant.activate();
        assertTrue(tenant.isActive());
    }

    @Test
    void deveDesativarTenant() {
        Tenant tenant = validBuilder().build();
        tenant.deactivate();
        assertFalse(tenant.isActive());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        Tenant tenant = validBuilder().build();
        tenant.activate();
        tenant.activate();
        assertTrue(tenant.isActive());
    }

    @Test
    void deveAtualizarNome() {
        Tenant tenant = validBuilder().build();
        tenant.updateName("Acme S.A.");
        assertEquals("Acme S.A.", tenant.getName());
    }

    @Test
    void deveLancarExcecaoAoAtualizarNomeParaVazio() {
        Tenant tenant = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> tenant.updateName(""));
        assertEquals(Tenant.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void codigoDeveSerImutavelAposCriacao() {
        Tenant tenant = validBuilder().build();
        TenantCode originalCode = tenant.getCode();

        tenant.updateName("Outro nome");

        assertEquals(originalCode, tenant.getCode());
    }
}
