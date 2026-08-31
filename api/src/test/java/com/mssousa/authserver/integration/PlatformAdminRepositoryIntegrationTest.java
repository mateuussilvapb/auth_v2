package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class PlatformAdminRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private PlatformAdminRepository platformAdminRepository;

    private PlatformAdmin createAndSave(String username, String email) {
        PlatformAdmin admin = PlatformAdmin.builder()
                .id(PlatformAdminId.of(idGenerator.generate()))
                .username(Username.of(username))
                .email(Email.of(email))
                .password(Password.fromPlainText("senhaSegura123"))
                .name(username)
                .build();
        return platformAdminRepository.save(admin);
    }

    @Test
    void deveSalvarEBuscarPorUsername() {
        createAndSave("root_admin", "admin@seudominio.com");

        PlatformAdmin found = platformAdminRepository.findByUsername(Username.of("root_admin")).orElseThrow();
        assertEquals("admin@seudominio.com", found.getEmail().value());
    }

    @Test
    void deveContarAdminsAtivos() {
        // Contagem relativa a uma baseline, não a um valor fixo: a migration de seed
        // (Fase 10, V15) já insere um platform admin ativo em toda base nova, inclusive a
        // do Testcontainers usada aqui.
        long baseline = platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE);
        createAndSave("admin_um", "um@seudominio.com");
        createAndSave("admin_dois", "dois@seudominio.com");

        assertEquals(baseline + 2, platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE));
    }

    @Test
    void desativarNaoDeveContarComoAtivo() {
        long baseline = platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE);
        PlatformAdmin admin = createAndSave("admin_tres", "tres@seudominio.com");
        assertEquals(baseline + 1, platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE));

        admin.deactivate();
        platformAdminRepository.save(admin);

        assertEquals(baseline, platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE));
    }
}
