package com.mssousa.authserver.application.service.binding;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.in.ManageBindingUseCase;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.application.port.out.UserSystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.service.TenantConsistencyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BindingManagementService implements ManageBindingUseCase {

    private final UserRepository userRepository;
    private final SystemTenantRepository systemTenantRepository;
    private final UserSystemRepository userSystemRepository;
    private final SystemProfileRepository systemProfileRepository;
    private final UserSystemProfileRepository userSystemProfileRepository;
    private final IdGeneratorPort idGenerator;
    private final TenantConsistencyValidator tenantConsistencyValidator;

    @Override
    @Transactional
    public UserSystem bindUserToSystem(TenantId tenantId, UserId userId, SystemId systemId) {
        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));
        SystemTenant systemTenant = systemTenantRepository.findBySystemId(systemId)
                .orElseThrow(() -> new ResourceNotFoundException("Sistema não vinculado a nenhum tenant: " + systemId));

        tenantConsistencyValidator.validateSameTenant(user, systemTenant);

        if (userSystemRepository.findByTenantIdAndUserIdAndSystemId(tenantId, userId, systemId).isPresent()) {
            throw new DomainException("Usuário já está vinculado a este sistema");
        }

        UserSystem binding = UserSystem.builder()
                .id(UserSystemId.of(idGenerator.generate()))
                .userId(userId)
                .systemId(systemId)
                .tenantId(tenantId)
                .build();

        return userSystemRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystem activateUserSystem(TenantId tenantId, UserSystemId id) {
        UserSystem binding = findUserSystemOrThrow(tenantId, id);
        binding.activate();
        return userSystemRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystem deactivateUserSystem(TenantId tenantId, UserSystemId id) {
        UserSystem binding = findUserSystemOrThrow(tenantId, id);
        binding.deactivate();
        return userSystemRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystem blockUserSystem(TenantId tenantId, UserSystemId id) {
        UserSystem binding = findUserSystemOrThrow(tenantId, id);
        binding.block();
        return userSystemRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystemProfile bindProfileToUserSystem(TenantId tenantId, UserSystemId userSystemId, SystemProfileId profileId) {
        UserSystem userSystem = findUserSystemOrThrow(tenantId, userSystemId);

        SystemProfile profile = systemProfileRepository.findBySystemIdAndId(userSystem.getSystemId(), profileId)
                .orElseThrow(() -> new DomainException("Perfil não pertence ao sistema deste vínculo"));

        if (userSystemProfileRepository.findByUserSystemIdAndSystemProfileId(userSystemId, profile.getId()).isPresent()) {
            throw new DomainException("Usuário já possui este perfil neste sistema");
        }

        UserSystemProfile binding = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(idGenerator.generate()))
                .userSystemId(userSystemId)
                .systemProfileId(profile.getId())
                .build();

        return userSystemProfileRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystemProfile activateUserSystemProfile(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id) {
        UserSystemProfile binding = findUserSystemProfileOrThrow(tenantId, userSystemId, id);
        binding.activate();
        return userSystemProfileRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystemProfile deactivateUserSystemProfile(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id) {
        UserSystemProfile binding = findUserSystemProfileOrThrow(tenantId, userSystemId, id);
        binding.deactivate();
        return userSystemProfileRepository.save(binding);
    }

    @Override
    @Transactional
    public UserSystemProfile blockUserSystemProfile(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id) {
        UserSystemProfile binding = findUserSystemProfileOrThrow(tenantId, userSystemId, id);
        binding.block();
        return userSystemProfileRepository.save(binding);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSystem> listUserSystems(TenantId tenantId, UserId userId, Pageable pageable) {
        return userSystemRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSystemProfile> listUserSystemProfiles(TenantId tenantId, UserSystemId userSystemId) {
        findUserSystemOrThrow(tenantId, userSystemId);
        return userSystemProfileRepository.findByUserSystemId(userSystemId);
    }

    private UserSystem findUserSystemOrThrow(TenantId tenantId, UserSystemId id) {
        return userSystemRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo usuário-sistema não encontrado: " + id));
    }

    private UserSystemProfile findUserSystemProfileOrThrow(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id) {
        findUserSystemOrThrow(tenantId, userSystemId);
        return userSystemProfileRepository.findByUserSystemIdAndId(userSystemId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo usuário-perfil não encontrado: " + id));
    }
}
