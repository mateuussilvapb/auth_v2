package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.TenantEntity;
import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemTenantJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.TenantJpaRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SystemTenantRepositoryImpl implements SystemTenantRepository {

    private final SystemTenantJpaRepository jpaRepository;
    private final TenantJpaRepository tenantJpaRepository;
    private final SystemJpaRepository systemJpaRepository;
    private final AuthMapper mapper;

    @Override
    public SystemTenant save(SystemTenant systemTenant) {
        TenantEntity tenant = tenantJpaRepository.findById(systemTenant.getTenantId().value())
                .orElseThrow(() -> new IllegalStateException("Tenant não encontrado: " + systemTenant.getTenantId()));
        SystemEntity system = systemJpaRepository.findById(systemTenant.getSystemId().value())
                .orElseThrow(() -> new IllegalStateException("System não encontrado: " + systemTenant.getSystemId()));

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(systemTenant, tenant, system)));
    }

    @Override
    public Optional<SystemTenant> findById(SystemTenantId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<SystemTenant> findBySystemId(SystemId systemId) {
        return jpaRepository.findBySystemId(systemId.value()).map(mapper::toDomain);
    }

    @Override
    public Page<SystemTenant> findByTenantId(TenantId tenantId, Pageable pageable) {
        return jpaRepository.findByTenantId(tenantId.value(), pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(SystemTenantId id) {
        jpaRepository.deleteById(id.value());
    }
}
