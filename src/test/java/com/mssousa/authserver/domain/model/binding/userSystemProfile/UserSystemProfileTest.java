package com.mssousa.authserver.domain.model.binding.userSystemProfile;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSystemProfileTest {

    private UserSystemProfile.Builder validBuilder() {
        return UserSystemProfile.builder()
                .id(UserSystemProfileId.of(1L))
                .userSystemId(UserSystemId.of(1L))
                .systemProfileId(SystemProfileId.of(1L));
    }

    @Test
    void deveCriarVinculoValidoComStatusPadraoActive() {
        UserSystemProfile binding = validBuilder().build();

        assertEquals(UserSystemProfileId.of(1L), binding.getId());
        assertEquals(UserSystemId.of(1L), binding.getUserSystemId());
        assertEquals(SystemProfileId.of(1L), binding.getSystemProfileId());
        assertTrue(binding.isActive());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystemProfile.builder().userSystemId(UserSystemId.of(1L)).systemProfileId(SystemProfileId.of(1L)).build());
        assertEquals(UserSystemProfile.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoUserSystemIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystemProfile.builder().id(UserSystemProfileId.of(1L)).systemProfileId(SystemProfileId.of(1L)).build());
        assertEquals(UserSystemProfile.ERROR_USER_SYSTEM_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSystemProfileIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> UserSystemProfile.builder().id(UserSystemProfileId.of(1L)).userSystemId(UserSystemId.of(1L)).build());
        assertEquals(UserSystemProfile.ERROR_SYSTEM_PROFILE_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtivarDesativarEBloquear() {
        UserSystemProfile binding = validBuilder().build();

        binding.deactivate();
        assertTrue(binding.isInactive());

        binding.block();
        assertTrue(binding.isBlocked());

        binding.activate();
        assertTrue(binding.isActive());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        UserSystemProfile binding = validBuilder().status(BindingStatus.BLOCKED).build();
        binding.activate();
        binding.activate();
        assertTrue(binding.isActive());
    }

    @Test
    void validateAccessNaoLancaExcecaoQuandoAtivo() {
        UserSystemProfile binding = validBuilder().build();
        assertDoesNotThrow(binding::validateAccess);
    }

    @Test
    void validateAccessLancaExcecaoQuandoInativo() {
        UserSystemProfile binding = validBuilder().status(BindingStatus.INACTIVE).build();
        DomainException exception = assertThrows(DomainException.class, binding::validateAccess);
        assertEquals(UserSystemProfile.ERROR_INACTIVE_BINDING, exception.getMessage());
    }
}
