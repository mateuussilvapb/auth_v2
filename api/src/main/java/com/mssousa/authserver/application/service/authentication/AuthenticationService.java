package com.mssousa.authserver.application.service.authentication;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.port.in.AuthenticateUserUseCase;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.application.port.out.UserSystemRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.Username;
import com.mssousa.authserver.domain.service.AccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementação de {@link AuthenticateUserUseCase}. Qualquer falha em qualquer etapa —
 * client_id desconhecido, usuário inexistente, senha errada, ou qualquer nível da
 * cascata de status inativo (seção 3.4) — resulta exatamente na mesma
 * {@link AuthenticationFailedException}, sem exceção. Não adicionar branches que
 * retornem mensagens diferentes por tipo de falha.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticateUserUseCase {

    private final SystemRepository systemRepository;
    private final SystemTenantRepository systemTenantRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AccessValidator accessValidator;

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String clientId, String usernameOrEmail, String plainPassword) {
        System system = resolveSystem(clientId);
        SystemTenant systemTenant = resolveSystemTenant(system);
        Tenant tenant = resolveTenant(systemTenant);
        User user = resolveUser(tenant.getId(), usernameOrEmail);
        UserSystem userSystem = resolveUserSystem(tenant.getId(), user, system);

        validateCascade(tenant, system, systemTenant, user, userSystem);

        if (!user.verifyPassword(plainPassword)) {
            user.registerFailedLoginAttempt();
            userRepository.save(user);
            throw new AuthenticationFailedException();
        }

        user.registerSuccessfulLogin();
        userRepository.save(user);

        return new AuthenticatedUser(user.getId(), tenant.getId(), system.getId(),
                user.getUsername(), user.getEmail(), user.getName());
    }

    private System resolveSystem(String clientId) {
        try {
            return systemRepository.findByClientId(ClientId.of(clientId))
                    .orElseThrow(AuthenticationFailedException::new);
        } catch (DomainException e) {
            throw new AuthenticationFailedException();
        }
    }

    private SystemTenant resolveSystemTenant(System system) {
        return systemTenantRepository.findBySystemId(system.getId())
                .orElseThrow(AuthenticationFailedException::new);
    }

    private Tenant resolveTenant(SystemTenant systemTenant) {
        return tenantRepository.findById(systemTenant.getTenantId())
                .orElseThrow(AuthenticationFailedException::new);
    }

    private User resolveUser(TenantId tenantId, String usernameOrEmail) {
        Optional<User> byUsername = tryOf(() -> Username.of(usernameOrEmail))
                .flatMap(username -> userRepository.findByTenantIdAndUsername(tenantId, username));

        if (byUsername.isPresent()) {
            return byUsername.get();
        }

        return tryOf(() -> Email.of(usernameOrEmail))
                .flatMap(email -> userRepository.findByTenantIdAndEmail(tenantId, email))
                .orElseThrow(AuthenticationFailedException::new);
    }

    private UserSystem resolveUserSystem(TenantId tenantId, User user, System system) {
        return userSystemRepository.findByTenantIdAndUserIdAndSystemId(tenantId, user.getId(), system.getId())
                .orElseThrow(AuthenticationFailedException::new);
    }

    private void validateCascade(Tenant tenant, System system, SystemTenant systemTenant, User user, UserSystem userSystem) {
        try {
            accessValidator.validateLoginAccess(tenant, system, systemTenant, user, userSystem);
        } catch (DomainException e) {
            throw new AuthenticationFailedException();
        }
    }

    private <T> Optional<T> tryOf(java.util.function.Supplier<T> factory) {
        try {
            return Optional.of(factory.get());
        } catch (DomainException e) {
            return Optional.empty();
        }
    }
}
