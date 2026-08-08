package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.System;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class SystemProfileRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private SystemProfileRepository systemProfileRepository;

    private SystemProfile createAndSave(System system, String code) {
        SystemProfile profile = SystemProfile.builder()
                .id(SystemProfileId.of(idGenerator.generate()))
                .systemId(system.getId())
                .code(ProfileCode.of(code))
                .build();
        return systemProfileRepository.save(profile);
    }

    @Test
    void deveSalvarEBuscarPorSistemaECodigo() {
        System system = createAndSaveSystem("CRM_ACME");
        createAndSave(system, "ADMIN");

        SystemProfile found = systemProfileRepository.findBySystemIdAndCode(system.getId(), ProfileCode.of("ADMIN")).orElseThrow();
        assertEquals("ADMIN", found.getCode().value());
    }

    @Test
    void mesmoCodigoDevePersistirEmSistemasDiferentes() {
        System sistemaA = createAndSaveSystem("CRM_ACME");
        System sistemaB = createAndSaveSystem("CRM_GLOBEX");

        createAndSave(sistemaA, "ADMIN");
        createAndSave(sistemaB, "ADMIN");

        assertTrue(systemProfileRepository.findBySystemIdAndCode(sistemaA.getId(), ProfileCode.of("ADMIN")).isPresent());
        assertTrue(systemProfileRepository.findBySystemIdAndCode(sistemaB.getId(), ProfileCode.of("ADMIN")).isPresent());
    }

    @Test
    void deveListarPerfisDeUmSistema() {
        System system = createAndSaveSystem("BACKOFFICE_ACME");
        createAndSave(system, "ADMIN");
        createAndSave(system, "FINANCEIRO");

        assertEquals(2, systemProfileRepository.findBySystemId(system.getId()).size());
    }
}
