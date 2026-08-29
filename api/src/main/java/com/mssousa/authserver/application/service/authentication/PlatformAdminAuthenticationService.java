package com.mssousa.authserver.application.service.authentication;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.port.in.AuthenticatePlatformAdminUseCase;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PlatformAdminAuthenticationService implements AuthenticatePlatformAdminUseCase {

    private final PlatformAdminRepository platformAdminRepository;

    @Override
    @Transactional(readOnly = true)
    public PlatformAdmin authenticate(String usernameOrEmail, String plainPassword) {
        PlatformAdmin admin = resolveAdmin(usernameOrEmail);

        if (!admin.canLogin() || !admin.verifyPassword(plainPassword)) {
            throw new AuthenticationFailedException();
        }

        return admin;
    }

    private PlatformAdmin resolveAdmin(String usernameOrEmail) {
        Optional<PlatformAdmin> byUsername = tryOf(() -> Username.of(usernameOrEmail))
                .flatMap(platformAdminRepository::findByUsername);

        if (byUsername.isPresent()) {
            return byUsername.get();
        }

        return tryOf(() -> Email.of(usernameOrEmail))
                .flatMap(platformAdminRepository::findByEmail)
                .orElseThrow(AuthenticationFailedException::new);
    }

    private <T> Optional<T> tryOf(Supplier<T> factory) {
        try {
            return Optional.of(factory.get());
        } catch (DomainException e) {
            return Optional.empty();
        }
    }
}
