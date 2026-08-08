package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.entity.TenantEntity;
import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.TenantJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.UserJpaRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final TenantJpaRepository tenantJpaRepository;
    private final AuthMapper mapper;

    @Override
    public User save(User user) {
        TenantEntity tenant = tenantJpaRepository.findById(user.getTenantId().value())
                .orElseThrow(() -> new IllegalStateException("Tenant não encontrado: " + user.getTenantId()));

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(user, tenant)));
    }

    @Override
    public Optional<User> findByTenantIdAndId(TenantId tenantId, UserId id) {
        return jpaRepository.findByTenantIdAndId(tenantId.value(), id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantIdAndUsername(TenantId tenantId, Username username) {
        return jpaRepository.findByTenantIdAndUsername(tenantId.value(), username.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantIdAndEmail(TenantId tenantId, Email email) {
        return jpaRepository.findByTenantIdAndEmail(tenantId.value(), email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndUsername(TenantId tenantId, Username username) {
        return jpaRepository.existsByTenantIdAndUsername(tenantId.value(), username.value());
    }

    @Override
    public boolean existsByTenantIdAndEmail(TenantId tenantId, Email email) {
        return jpaRepository.existsByTenantIdAndEmail(tenantId.value(), email.value());
    }

    @Override
    public Page<User> findByTenantId(TenantId tenantId, Pageable pageable) {
        return jpaRepository.findByTenantId(tenantId.value(), pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteByTenantIdAndId(TenantId tenantId, UserId id) {
        jpaRepository.deleteByTenantIdAndId(tenantId.value(), id.value());
    }
}
