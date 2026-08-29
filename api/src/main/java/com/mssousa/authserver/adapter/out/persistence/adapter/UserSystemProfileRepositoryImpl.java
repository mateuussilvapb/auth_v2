package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemProfileEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemEntity;
import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.SystemProfileJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.UserSystemJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.UserSystemProfileJpaRepository;
import com.mssousa.authserver.application.port.out.UserSystemProfileRepository;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserSystemProfileRepositoryImpl implements UserSystemProfileRepository {

    private final UserSystemProfileJpaRepository jpaRepository;
    private final UserSystemJpaRepository userSystemJpaRepository;
    private final SystemProfileJpaRepository systemProfileJpaRepository;
    private final AuthMapper mapper;

    @Override
    public UserSystemProfile save(UserSystemProfile userSystemProfile) {
        UserSystemEntity userSystem = userSystemJpaRepository.findById(userSystemProfile.getUserSystemId().value())
                .orElseThrow(() -> new IllegalStateException("UserSystem não encontrado: " + userSystemProfile.getUserSystemId()));
        SystemProfileEntity systemProfile = systemProfileJpaRepository.findById(userSystemProfile.getSystemProfileId().value())
                .orElseThrow(() -> new IllegalStateException("SystemProfile não encontrado: " + userSystemProfile.getSystemProfileId()));

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(userSystemProfile, userSystem, systemProfile)));
    }

    @Override
    public Optional<UserSystemProfile> findByUserSystemIdAndId(UserSystemId userSystemId, UserSystemProfileId id) {
        return jpaRepository.findByUserSystemIdAndId(userSystemId.value(), id.value()).map(mapper::toDomain);
    }

    @Override
    public List<UserSystemProfile> findByUserSystemId(UserSystemId userSystemId) {
        return jpaRepository.findByUserSystemId(userSystemId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<UserSystemProfile> findByUserSystemIdAndSystemProfileId(UserSystemId userSystemId, SystemProfileId systemProfileId) {
        return jpaRepository.findByUserSystemIdAndSystemProfileId(userSystemId.value(), systemProfileId.value())
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByUserSystemIdAndId(UserSystemId userSystemId, UserSystemProfileId id) {
        jpaRepository.deleteByUserSystemIdAndId(userSystemId.value(), id.value());
    }
}
