package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;

class UserSystemRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private SystemTenantRepository systemTenantRepository;

    @Autowired
    private UserSystemRepository userSystemRepository;

    @Test
    void deveSalvarEBuscarPorTenantUsuarioESistema() {
        Tenant tenant = createAndSaveTenant("acme");
        System system = createAndSaveSystem("CRM_ACME");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate())).tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "joao_silva", "joao@acme.com");

        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId())
                .build();
        userSystemRepository.save(userSystem);

        UserSystem found = userSystemRepository.findByTenantIdAndUserIdAndSystemId(tenant.getId(), user.getId(), system.getId())
                .orElseThrow();
        assertTrue(found.isActive());
    }

    @Test
    void deveListarVinculosDeUmUsuario() {
        Tenant tenant = createAndSaveTenant("globex");
        System system = createAndSaveSystem("ERP_GLOBEX");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate())).tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "maria_souza", "maria@globex.com");

        userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId())
                .build());

        assertEquals(1, userSystemRepository.findByTenantIdAndUserId(tenant.getId(), user.getId(), PageRequest.of(0, 10))
                .getTotalElements());
    }
}
