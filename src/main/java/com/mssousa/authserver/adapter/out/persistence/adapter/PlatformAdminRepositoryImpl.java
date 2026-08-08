package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.PlatformAdminJpaRepository;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PlatformAdminRepositoryImpl implements PlatformAdminRepository {

    private final PlatformAdminJpaRepository jpaRepository;
    private final AuthMapper mapper;

    @Override
    public PlatformAdmin save(PlatformAdmin platformAdmin) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(platformAdmin)));
    }

    @Override
    public Optional<PlatformAdmin> findById(PlatformAdminId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<PlatformAdmin> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<PlatformAdmin> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return jpaRepository.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    public long countByStatus(PlatformAdminStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public Page<PlatformAdmin> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(PlatformAdminId id) {
        jpaRepository.deleteById(id.value());
    }
}
