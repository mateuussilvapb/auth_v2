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
        createAndSave("admin_um", "um@seudominio.com");
        createAndSave("admin_dois", "dois@seudominio.com");

        assertEquals(2, platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE));
    }

    @Test
    void desativarNaoDeveContarComoAtivo() {
        PlatformAdmin admin = createAndSave("admin_tres", "tres@seudominio.com");
        admin.deactivate();
        platformAdminRepository.save(admin);

        assertEquals(0, platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE));
    }
}
