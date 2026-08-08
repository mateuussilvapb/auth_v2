package com.mssousa.authserver.application.service.profile;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.service.ProfileUniquenessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileManagementServiceTest {

    @Mock
    private SystemProfileRepository profileRepository;
    @Mock
    private SystemRepository systemRepository;
    @Mock
    private IdGeneratorPort idGenerator;

    private ProfileManagementService service;

    @BeforeEach
    void setUp() {
        service = new ProfileManagementService(profileRepository, systemRepository, idGenerator, new ProfileUniquenessPolicy());
    }

    private System existingSystem() {
        return System.builder().id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build();
    }

    @Test
    void deveCriarPerfilQuandoCodigoDisponivelNoSistema() {
        when(systemRepository.findById(SystemId.of(1L))).thenReturn(Optional.of(existingSystem()));
        when(profileRepository.findBySystemId(SystemId.of(1L))).thenReturn(List.of());
        when(idGenerator.generate()).thenReturn(1L);
        when(profileRepository.save(any(SystemProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemProfile created = service.createProfile(SystemId.of(1L), "ADMIN", "Administrador");

        assertEquals(ProfileCode.of("ADMIN"), created.getCode());
    }

    @Test
    void deveLancarExcecaoAoCriarPerfilParaSistemaInexistente() {
        when(systemRepository.findById(SystemId.of(99L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createProfile(SystemId.of(99L), "ADMIN", ""));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCriarPerfilComCodigoDuplicadoNoMesmoSistema() {
        SystemProfile existente = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();
        when(systemRepository.findById(SystemId.of(1L))).thenReturn(Optional.of(existingSystem()));
        when(profileRepository.findBySystemId(SystemId.of(1L))).thenReturn(List.of(existente));

        assertThrows(DomainException.class, () -> service.createProfile(SystemId.of(1L), "ADMIN", "Duplicado"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void deveAtualizarDescricao() {
        SystemProfile profile = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();
        when(profileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(1L))).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(SystemProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemProfile updated = service.updateProfileDescription(SystemId.of(1L), SystemProfileId.of(1L), "Nova descrição");
        assertEquals("Nova descrição", updated.getDescription());
    }

    @Test
    void deveAtivarEDesativarPerfil() {
        SystemProfile profile = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();
        when(profileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(1L))).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(SystemProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(service.deactivateProfile(SystemId.of(1L), SystemProfileId.of(1L)).isActive());
        assertTrue(service.activateProfile(SystemId.of(1L), SystemProfileId.of(1L)).isActive());
    }

    @Test
    void deveLancarExcecaoAoBuscarPerfilInexistente() {
        when(profileRepository.findBySystemIdAndId(SystemId.of(1L), SystemProfileId.of(99L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProfile(SystemId.of(1L), SystemProfileId.of(99L)));
    }
}
