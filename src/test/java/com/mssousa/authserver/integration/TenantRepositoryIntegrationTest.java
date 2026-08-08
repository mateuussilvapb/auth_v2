package com.mssousa.authserver.integration;

import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Test
    void deveSalvarEBuscarPorId() {
        Tenant saved = createAndSaveTenant("acme");

        Tenant found = tenantRepository.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getCode(), found.getCode());
        assertEquals(TenantStatus.ACTIVE, found.getStatus());
    }

    @Test
    void deveBuscarPorCode() {
        createAndSaveTenant("globex");

        Tenant found = tenantRepository.findByCode(TenantCode.of("globex")).orElseThrow();
        assertEquals("globex", found.getCode().value());
    }

    @Test
    void existsByCodeDeveRefletirEstadoReal() {
        assertFalse(tenantRepository.existsByCode(TenantCode.of("inexistente")));
        createAndSaveTenant("initech");
        assertTrue(tenantRepository.existsByCode(TenantCode.of("initech")));
    }

    @Test
    void deveAtualizarStatusAoSalvarNovamente() {
        Tenant saved = createAndSaveTenant("umbrella");
        saved.deactivate();
        tenantRepository.save(saved);

        Tenant found = tenantRepository.findById(saved.getId()).orElseThrow();
        assertEquals(TenantStatus.INACTIVE, found.getStatus());
    }
}
