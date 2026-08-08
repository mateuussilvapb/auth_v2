package com.mssousa.authserver.application.service.resetpassword;

import com.mssousa.authserver.application.port.in.ResetPasswordUseCase;
import com.mssousa.authserver.application.port.out.EmailSenderPort;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.PasswordResetTokenRepository;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.ResetTokenValue;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class ResetPasswordService implements ResetPasswordUseCase {

    public static final String ERROR_INVALID_TOKEN = "Token de redefinição inválido ou expirado";

    private static final int TOKEN_TTL_MINUTES = 30;

    private final SystemRepository systemRepository;
    private final SystemTenantRepository systemTenantRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final IdGeneratorPort idGenerator;
    private final EmailSenderPort emailSender;
    private final String resetPasswordUrl;

    public ResetPasswordService(SystemRepository systemRepository,
                                 SystemTenantRepository systemTenantRepository,
                                 UserRepository userRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository,
                                 IdGeneratorPort idGenerator,
                                 EmailSenderPort emailSender,
                                 @Value("${authserver.frontend.reset-password-url}") String resetPasswordUrl) {
        this.systemRepository = systemRepository;
        this.systemTenantRepository = systemTenantRepository;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.idGenerator = idGenerator;
        this.emailSender = emailSender;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Override
    @Transactional
    public void requestReset(String clientId, String usernameOrEmail) {
        resolveTenantId(clientId)
                .flatMap(tenantId -> resolveUser(tenantId, usernameOrEmail))
                .ifPresent(this::generateAndSendToken);
    }

    private Optional<TenantId> resolveTenantId(String clientId) {
        return tryOf(() -> ClientId.of(clientId))
                .flatMap(systemRepository::findByClientId)
                .flatMap(system -> systemTenantRepository.findBySystemId(system.getId()))
                .map(SystemTenant::getTenantId);
    }

    private void generateAndSendToken(User user) {
        Instant expiresAt = Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES);
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(idGenerator.generate()), user.getId(), expiresAt);

        passwordResetTokenRepository.save(generated.token());

        String resetLink = resetPasswordUrl + "?token=" + generated.rawValue();
        emailSender.sendPasswordResetEmail(user.getEmail().value(), user.getName(), resetLink);
    }

    @Override
    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        PasswordResetToken token = resolveToken(rawToken);

        try {
            token.validateUsable();
        } catch (DomainException e) {
            throw new DomainException(ERROR_INVALID_TOKEN);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new DomainException(ERROR_INVALID_TOKEN));

        user.changePassword(Password.fromPlainText(newPassword));
        userRepository.save(user);

        token.markAsUsed();
        passwordResetTokenRepository.save(token);
    }

    private PasswordResetToken resolveToken(String rawToken) {
        try {
            ResetTokenValue hash = ResetTokenValue.ofRawToken(rawToken);
            return passwordResetTokenRepository.findByValue(hash)
                    .orElseThrow(() -> new DomainException(ERROR_INVALID_TOKEN));
        } catch (DomainException e) {
            throw new DomainException(ERROR_INVALID_TOKEN);
        }
    }

    private Optional<User> resolveUser(TenantId tenantId, String usernameOrEmail) {
        Optional<User> byUsername = tryOf(() -> Username.of(usernameOrEmail))
                .flatMap(username -> userRepository.findByTenantIdAndUsername(tenantId, username));

        if (byUsername.isPresent()) {
            return byUsername;
        }

        return tryOf(() -> Email.of(usernameOrEmail))
                .flatMap(email -> userRepository.findByTenantIdAndEmail(tenantId, email));
    }

    private <T> Optional<T> tryOf(Supplier<T> factory) {
        try {
            return Optional.of(factory.get());
        } catch (DomainException e) {
            return Optional.empty();
        }
    }
}
