package com.mssousa.authserver.application.service.system;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.ClientSecret;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
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
class SystemManagementServiceTest {

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemTenantRepository systemTenantRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private IdGeneratorPort idGenerator;

    private SystemManagementService service;

    @BeforeEach
    void setUp() {
        service = new SystemManagementService(systemRepository, systemTenantRepository, tenantRepository, idGenerator);
    }

    private System existingSystem() {
        return System.builder()
                .id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM Acme")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build();
    }

    @Test
    void deveCriarSistemaEVincularAoTenant() {
        Tenant tenant = Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build();
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(tenant));
        when(systemRepository.existsByClientId(ClientId.of("CRM_ACME"))).thenReturn(false);
        when(idGenerator.generate()).thenReturn(10L, 20L);
        when(systemRepository.save(any(System.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemTenantRepository.save(any(SystemTenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        System created = service.createSystem(TenantId.of(1L), "CRM_ACME", "CRM Acme", true, null,
                List.of("https://crm.acme.com/callback"));

        assertEquals(ClientId.of("CRM_ACME"), created.getClientId());
        verify(systemTenantRepository).save(argThat(binding ->
                binding.getTenantId().equals(TenantId.of(1L)) && binding.getSystemId().equals(created.getId())));
    }

    @Test
    void deveLancarExcecaoAoCriarSistemaParaTenantInexistente() {
        when(tenantRepository.findById(TenantId.of(99L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createSystem(
                TenantId.of(99L), "CRM_ACME", "CRM", true, null, List.of("https://crm.acme.com/callback")));
        verify(systemRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCriarSistemaComClientIdDuplicado() {
        when(tenantRepository.findById(TenantId.of(1L)))
                .thenReturn(Optional.of(Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build()));
        when(systemRepository.existsByClientId(ClientId.of("CRM_ACME"))).thenReturn(true);

        assertThrows(DomainException.class, () -> service.createSystem(
                TenantId.of(1L), "CRM_ACME", "CRM", true, null, List.of("https://crm.acme.com/callback")));
        verify(systemTenantRepository, never()).save(any());
    }

    @Test
    void deveAdicionarRedirectUri() {
        System system = existingSystem();
        when(systemRepository.findById(SystemId.of(1L))).thenReturn(Optional.of(system));
        when(systemRepository.save(any(System.class))).thenAnswer(invocation -> invocation.getArgument(0));

        System updated = service.addRedirectUri(SystemId.of(1L), "https://crm.acme.com/dev-callback");
        assertEquals(2, updated.getRedirectUris().size());
    }

    @Test
    void deveRotacionarSecretDeSistemaConfidencial() {
        System confidential = System.builder()
                .id(SystemId.of(2L)).clientId(ClientId.of("BACKOFFICE")).name("Backoffice")
                .publicClient(false).clientSecret(ClientSecret.fromPlainText("secret-antigo"))
                .redirectUri(RedirectUri.of("https://backoffice.acme.com/callback")).build();
        when(systemRepository.findById(SystemId.of(2L))).thenReturn(Optional.of(confidential));
        when(systemRepository.save(any(System.class))).thenAnswer(invocation -> invocation.getArgument(0));

        System updated = service.rotateSecret(SystemId.of(2L), "secret-novo");
        assertTrue(updated.verifyClientSecret("secret-novo"));
    }

    @Test
    void deveLancarExcecaoAoOperarSistemaInexistente() {
        when(systemRepository.findById(SystemId.of(99L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getSystem(SystemId.of(99L)));
    }
}
