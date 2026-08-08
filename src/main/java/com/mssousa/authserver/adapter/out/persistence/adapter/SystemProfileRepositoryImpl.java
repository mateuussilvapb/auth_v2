package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemProfileJpaRepository;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SystemProfileRepositoryImpl implements SystemProfileRepository {

    private final SystemProfileJpaRepository jpaRepository;
    private final SystemJpaRepository systemJpaRepository;
    private final AuthMapper mapper;

    @Override
    public SystemProfile save(SystemProfile profile) {
        SystemEntity system = systemJpaRepository.findById(profile.getSystemId().value())
                .orElseThrow(() -> new IllegalStateException("System não encontrado: " + profile.getSystemId()));

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(profile, system)));
    }

    @Override
    public Optional<SystemProfile> findBySystemIdAndId(SystemId systemId, SystemProfileId id) {
        return jpaRepository.findBySystemIdAndId(systemId.value(), id.value()).map(mapper::toDomain);
    }

    @Override
    public List<SystemProfile> findBySystemId(SystemId systemId) {
        return jpaRepository.findBySystemId(systemId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SystemProfile> findBySystemIdAndCode(SystemId systemId, ProfileCode code) {
        return jpaRepository.findBySystemIdAndCode(systemId.value(), code.value()).map(mapper::toDomain);
    }

    @Override
    public void deleteBySystemIdAndId(SystemId systemId, SystemProfileId id) {
        jpaRepository.deleteBySystemIdAndId(systemId.value(), id.value());
    }
}
