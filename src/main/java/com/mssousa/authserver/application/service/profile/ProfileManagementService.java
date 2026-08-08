package com.mssousa.authserver.application.service.profile;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.in.ManageProfileUseCase;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.service.ProfileUniquenessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileManagementService implements ManageProfileUseCase {

    private final SystemProfileRepository profileRepository;
    private final SystemRepository systemRepository;
    private final IdGeneratorPort idGenerator;
    private final ProfileUniquenessPolicy profileUniquenessPolicy;

    @Override
    @Transactional
    public SystemProfile createProfile(SystemId systemId, String code, String description) {
        if (systemRepository.findById(systemId).isEmpty()) {
            throw new ResourceNotFoundException("Sistema não encontrado: " + systemId);
        }

        ProfileCode profileCode = ProfileCode.of(code);
        profileUniquenessPolicy.validateUniqueForCreate(profileCode, profileRepository.findBySystemId(systemId));

        SystemProfile profile = SystemProfile.builder()
                .id(SystemProfileId.of(idGenerator.generate()))
                .systemId(systemId)
                .code(profileCode)
                .description(description)
                .build();

        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public SystemProfile updateProfileDescription(SystemId systemId, SystemProfileId id, String newDescription) {
        SystemProfile profile = findByIdOrThrow(systemId, id);
        profile.updateDescription(newDescription);
        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public SystemProfile activateProfile(SystemId systemId, SystemProfileId id) {
        SystemProfile profile = findByIdOrThrow(systemId, id);
        profile.activate();
        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public SystemProfile deactivateProfile(SystemId systemId, SystemProfileId id) {
        SystemProfile profile = findByIdOrThrow(systemId, id);
        profile.deactivate();
        return profileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemProfile getProfile(SystemId systemId, SystemProfileId id) {
        return findByIdOrThrow(systemId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemProfile> listProfilesBySystem(SystemId systemId) {
        return profileRepository.findBySystemId(systemId);
    }

    private SystemProfile findByIdOrThrow(SystemId systemId, SystemProfileId id) {
        return profileRepository.findBySystemIdAndId(systemId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado: " + id));
    }
}
