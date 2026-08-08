package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemJpaRepository;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SystemRepositoryImpl implements SystemRepository {

    private final SystemJpaRepository jpaRepository;
    private final AuthMapper mapper;

    @Override
    public System save(System system) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(system)));
    }

    @Override
    public Optional<System> findById(SystemId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<System> findByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByClientId(ClientId clientId) {
        return jpaRepository.existsByClientId(clientId.value());
    }

    @Override
    public Page<System> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(SystemId id) {
        jpaRepository.deleteById(id.value());
    }
}
