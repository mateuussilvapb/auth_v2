package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;

class SystemTenantRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private SystemTenantRepository systemTenantRepository;

    @Test
    void deveSalvarEResolverTenantAPartirDoSistema() {
        Tenant tenant = createAndSaveTenant("acme");
        System system = createAndSaveSystem("CRM_ACME");

        SystemTenant binding = SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId())
                .systemId(system.getId())
                .build();
        systemTenantRepository.save(binding);

        SystemTenant found = systemTenantRepository.findBySystemId(system.getId()).orElseThrow();
        assertEquals(tenant.getId(), found.getTenantId());
    }

    @Test
    void deveListarSistemasDeUmTenant() {
        Tenant tenant = createAndSaveTenant("globex");
        System system = createAndSaveSystem("ERP_GLOBEX");

        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId())
                .systemId(system.getId())
                .build());

        assertEquals(1, systemTenantRepository.findByTenantId(tenant.getId(), PageRequest.of(0, 10)).getTotalElements());
    }
}
