package com.mssousa.authserver.application.service.tenant;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.exception.DomainException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantManagementServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private IdGeneratorPort idGenerator;

    private TenantManagementService service;

    @BeforeEach
    void setUp() {
        service = new TenantManagementService(tenantRepository, idGenerator);
    }

    private Tenant existingTenant() {
        return Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build();
    }

    @Test
    void deveCriarTenantQuandoCodigoDisponivel() {
        when(idGenerator.generate()).thenReturn(1L);
        when(tenantRepository.existsByCode(TenantCode.of("acme"))).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant created = service.createTenant("acme", "Acme Corp");

        assertEquals(TenantCode.of("acme"), created.getCode());
        assertEquals("Acme Corp", created.getName());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void deveLancarExcecaoAoCriarTenantComCodigoJaExistente() {
        when(tenantRepository.existsByCode(TenantCode.of("acme"))).thenReturn(true);

        DomainException exception = assertThrows(DomainException.class, () -> service.createTenant("acme", "Acme Corp"));
        assertTrue(exception.getMessage().contains("acme"));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void deveAtualizarNomeDoTenant() {
        Tenant tenant = existingTenant();
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant updated = service.updateTenant(TenantId.of(1L), "Acme S.A.");

        assertEquals("Acme S.A.", updated.getName());
    }

    @Test
    void deveLancarExcecaoAoAtualizarTenantInexistente() {
        when(tenantRepository.findById(TenantId.of(99L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateTenant(TenantId.of(99L), "Novo Nome"));
    }

    @Test
    void deveAtivarEDesativarTenant() {
        Tenant tenant = existingTenant();
        tenant.deactivate();
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant activated = service.activateTenant(TenantId.of(1L));
        assertTrue(activated.isActive());

        Tenant deactivated = service.deactivateTenant(TenantId.of(1L));
        assertFalse(deactivated.isActive());
    }

    @Test
    void deveBuscarTenantPorId() {
        when(tenantRepository.findById(TenantId.of(1L))).thenReturn(Optional.of(existingTenant()));

        Tenant found = service.getTenant(TenantId.of(1L));
        assertEquals(TenantId.of(1L), found.getId());
    }

    @Test
    void deveLancarExcecaoAoBuscarTenantInexistente() {
        when(tenantRepository.findById(TenantId.of(99L))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getTenant(TenantId.of(99L)));
    }
}
