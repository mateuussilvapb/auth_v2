package com.mssousa.authserver.application.service.authorization;

import com.mssousa.authserver.application.exception.AccessDeniedException;
import com.mssousa.authserver.application.model.AuthorizedUser;
import com.mssousa.authserver.application.port.in.AuthorizeUserUseCase;
import com.mssousa.authserver.application.port.out.SystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemProfileRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.service.AccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorizationService implements AuthorizeUserUseCase {

    private final UserSystemRepository userSystemRepository;
    private final UserSystemProfileRepository userSystemProfileRepository;
    private final SystemProfileRepository systemProfileRepository;
    private final AccessValidator accessValidator;

    @Override
    @Transactional(readOnly = true)
    public AuthorizedUser authorize(TenantId tenantId, UserId userId, SystemId systemId) {
        UserSystem userSystem = userSystemRepository.findByTenantIdAndUserIdAndSystemId(tenantId, userId, systemId)
                .orElseThrow(() -> new AccessDeniedException("Usuário não possui vínculo com este sistema"));

        List<String> profileCodes = userSystemProfileRepository.findByUserSystemId(userSystem.getId()).stream()
                .map(binding -> resolveActiveProfileCode(systemId, binding))
                .filter(java.util.Objects::nonNull)
                .toList();

        return new AuthorizedUser(userId, tenantId, systemId, profileCodes);
    }

    private String resolveActiveProfileCode(SystemId systemId, UserSystemProfile binding) {
        return systemProfileRepository.findBySystemIdAndId(systemId, binding.getSystemProfileId())
                .filter(profile -> isAccessible(binding, profile))
                .map(profile -> profile.getCode().value())
                .orElse(null);
    }

    private boolean isAccessible(UserSystemProfile binding, SystemProfile profile) {
        try {
            accessValidator.validateProfileAccess(binding, profile);
            return true;
        } catch (DomainException e) {
            return false;
        }
    }
}
