package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformAdminPolicyTest {

    private final PlatformAdminPolicy policy = new PlatformAdminPolicy();

    private PlatformAdmin activeAdmin() {
        return PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L))
                .username(Username.of("root_admin"))
                .email(Email.of("admin@seudominio.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("Administrador")
                .build();
    }

    @Test
    void devePermitirDesativarQuandoHaMaisDeUmAdminAtivo() {
        assertDoesNotThrow(() -> policy.validateCanDeactivate(activeAdmin(), 2L));
    }

    @Test
    void deveLancarExcecaoAoDesativarOUltimoAdminAtivo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> policy.validateCanDeactivate(activeAdmin(), 1L));
        assertEquals(PlatformAdminPolicy.ERROR_LAST_ACTIVE_ADMIN, exception.getMessage());
    }

    @Test
    void devePermitirDesativarAlvoJaInativoMesmoSendoOUnico() {
        PlatformAdmin inactiveAdmin = activeAdmin();
        inactiveAdmin.deactivate();

        assertDoesNotThrow(() -> policy.validateCanDeactivate(inactiveAdmin, 0L));
    }

    @Test
    void deveConsiderarStatusPadraoAoCriar() {
        PlatformAdmin admin = activeAdmin();
        assertEquals(PlatformAdminStatus.ACTIVE, admin.getStatus());
    }
}
