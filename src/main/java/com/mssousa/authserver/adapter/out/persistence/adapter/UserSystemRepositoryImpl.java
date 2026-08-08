package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserEntity;
import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.UserJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.UserSystemJpaRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserSystemRepositoryImpl implements UserSystemRepository {

    private final UserSystemJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final SystemJpaRepository systemJpaRepository;
    private final AuthMapper mapper;

    @Override
    public UserSystem save(UserSystem userSystem) {
        UserEntity user = userJpaRepository.findById(userSystem.getUserId().value())
                .orElseThrow(() -> new IllegalStateException("User não encontrado: " + userSystem.getUserId()));
        SystemEntity system = systemJpaRepository.findById(userSystem.getSystemId().value())
                .orElseThrow(() -> new IllegalStateException("System não encontrado: " + userSystem.getSystemId()));

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(userSystem, user, system)));
    }

    @Override
    public Optional<UserSystem> findByTenantIdAndId(TenantId tenantId, UserSystemId id) {
        return jpaRepository.findByTenantIdAndId(tenantId.value(), id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<UserSystem> findByTenantIdAndUserIdAndSystemId(TenantId tenantId, UserId userId, SystemId systemId) {
        return jpaRepository.findByTenantIdAndUserIdAndSystemId(tenantId.value(), userId.value(), systemId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Page<UserSystem> findByTenantIdAndUserId(TenantId tenantId, UserId userId, Pageable pageable) {
        return jpaRepository.findByTenantIdAndUserId(tenantId.value(), userId.value(), pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteByTenantIdAndId(TenantId tenantId, UserSystemId id) {
        jpaRepository.deleteByTenantIdAndId(tenantId.value(), id.value());
    }
}
