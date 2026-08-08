package com.mssousa.authserver.application.service.tenant;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.in.ManageTenantUseCase;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantManagementService implements ManageTenantUseCase {

    private final TenantRepository tenantRepository;
    private final IdGeneratorPort idGenerator;

    @Override
    @Transactional
    public Tenant createTenant(String code, String name) {
        TenantCode tenantCode = TenantCode.of(code);

        if (tenantRepository.existsByCode(tenantCode)) {
            throw new DomainException("Já existe um tenant com o código '" + tenantCode.value() + "'");
        }

        Tenant tenant = Tenant.builder()
                .id(TenantId.of(idGenerator.generate()))
                .code(tenantCode)
                .name(name)
                .build();

        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public Tenant updateTenant(TenantId id, String newName) {
        Tenant tenant = findByIdOrThrow(id);
        tenant.updateName(newName);
        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public Tenant activateTenant(TenantId id) {
        Tenant tenant = findByIdOrThrow(id);
        tenant.activate();
        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public Tenant deactivateTenant(TenantId id) {
        Tenant tenant = findByIdOrThrow(id);
        tenant.deactivate();
        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getTenant(TenantId id) {
        return findByIdOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tenant> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    private Tenant findByIdOrThrow(TenantId id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado: " + id));
    }
}
