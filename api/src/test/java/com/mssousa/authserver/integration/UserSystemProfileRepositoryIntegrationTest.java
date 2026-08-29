package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserSystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class UserSystemProfileRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private SystemTenantRepository systemTenantRepository;
    @Autowired
    private UserSystemRepository userSystemRepository;
    @Autowired
    private SystemProfileRepository systemProfileRepository;
    @Autowired
    private UserSystemProfileRepository userSystemProfileRepository;

    @Test
    void deveSalvarEBuscarPorVinculoEPerfil() {
        Tenant tenant = createAndSaveTenant("acme");
        System system = createAndSaveSystem("CRM_ACME");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate())).tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "joao_silva", "joao@acme.com");
        UserSystem userSystem = userSystemRepository.save(UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(user.getId()).systemId(system.getId()).tenantId(tenant.getId()).build());
        SystemProfile profile = systemProfileRepository.save(SystemProfile.builder()
                .id(SystemProfileId.of(idGenerator.generate())).systemId(system.getId()).code(ProfileCode.of("ADMIN")).build());

        UserSystemProfile binding = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(idGenerator.generate()))
                .userSystemId(userSystem.getId()).systemProfileId(profile.getId())
                .build();
        userSystemProfileRepository.save(binding);

        UserSystemProfile found = userSystemProfileRepository
                .findByUserSystemIdAndSystemProfileId(userSystem.getId(), profile.getId())
                .orElseThrow();
        assertTrue(found.isActive());
        assertEquals(1, userSystemProfileRepository.findByUserSystemId(userSystem.getId()).size());
    }
}
