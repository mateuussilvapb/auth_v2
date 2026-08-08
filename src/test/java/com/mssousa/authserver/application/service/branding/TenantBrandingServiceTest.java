package com.mssousa.authserver.application.service.branding;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.model.TenantBranding;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.ClientId;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantBrandingServiceTest {

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemTenantRepository systemTenantRepository;
    @Mock
    private TenantRepository tenantRepository;

    private TenantBrandingService service;

    @BeforeEach
    void setUp() {
        service = new TenantBrandingService(systemRepository, systemTenantRepository, tenantRepository);
    }

    private System activeSystem() {
        return System.builder().id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build();
    }

    private SystemTenant activeSystemTenant() {
        return SystemTenant.builder().id(SystemTenantId.of(1L)).tenantId(TenantId.of(1L)).systemId(SystemId.of(1L)).build();
    }

    private Tenant tenantWithBranding() {
        return Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme Corp")
                .logoUrl("https://acme.com/logo.png").build();
    }

    @Test
    void deveResolverBrandingParaClientIdValido() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(tenantWithBranding()));

        TenantBranding branding = service.resolveByClientId("CRM_ACME");

        assertEquals("Acme Corp", branding.tenantName());
        assertEquals("https://acme.com/logo.png", branding.logoUrl());
    }

    @Test
    void deveLancarExcecaoQuandoClientIdDesconhecido() {
        when(systemRepository.findByClientId(ClientId.of("DESCONHECIDO"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resolveByClientId("DESCONHECIDO"));
    }

    @Test
    void deveLancarExcecaoQuandoClientIdInvalido() {
        assertThrows(ResourceNotFoundException.class, () -> service.resolveByClientId(""));
    }

    @Test
    void deveLancarExcecaoQuandoSistemaSemVinculoDeTenant() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resolveByClientId("CRM_ACME"));
    }

    @Test
    void deveLancarExcecaoQuandoTenantNaoEncontrado() {
        when(systemRepository.findByClientId(ClientId.of("CRM_ACME"))).thenReturn(Optional.of(activeSystem()));
        when(systemTenantRepository.findBySystemId(SystemId.of(1L))).thenReturn(Optional.of(activeSystemTenant()));
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resolveByClientId("CRM_ACME"));
    }
}
