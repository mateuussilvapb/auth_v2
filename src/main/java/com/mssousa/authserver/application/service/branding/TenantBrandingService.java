package com.mssousa.authserver.application.service.branding;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.model.TenantBranding;
import com.mssousa.authserver.application.port.in.GetTenantBrandingUseCase;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link GetTenantBrandingUseCase}. Mesma cadeia de resolução de
 * {@code client_id} → {@code system} → {@code system_tenant} → {@code tenant} usada pelo
 * login (seção 7.1), mas sem cascata de status: branding é exibido mesmo que o tenant ou
 * o sistema estejam temporariamente inativos (o próprio login vai rejeitar depois).
 */
@Service
@RequiredArgsConstructor
public class TenantBrandingService implements GetTenantBrandingUseCase {

    private static final String ERROR_NOT_FOUND = "Nenhum tenant encontrado para o client_id informado";

    private final SystemRepository systemRepository;
    private final SystemTenantRepository systemTenantRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantBranding resolveByClientId(String clientId) {
        System system = resolveSystem(clientId);
        SystemTenant systemTenant = systemTenantRepository.findBySystemId(system.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND));
        Tenant tenant = tenantRepository.findById(systemTenant.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND));

        return new TenantBranding(tenant.getName(), tenant.getLogoUrl());
    }

    private System resolveSystem(String clientId) {
        try {
            return systemRepository.findByClientId(ClientId.of(clientId))
                    .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND));
        } catch (DomainException e) {
            throw new ResourceNotFoundException(ERROR_NOT_FOUND);
        }
    }
}
