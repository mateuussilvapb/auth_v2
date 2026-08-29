package com.mssousa.authserver.application.service.user;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.in.ManageUserUseCase;
import com.mssousa.authserver.application.port.out.EmailSenderPort;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.application.port.out.UserRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementService implements ManageUserUseCase {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final IdGeneratorPort idGenerator;
    private final EmailSenderPort emailSender;

    @Override
    @Transactional
    public User createUser(TenantId tenantId, String username, String email, String plainPassword, String name) {
        if (tenantRepository.findById(tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Tenant não encontrado: " + tenantId);
        }

        Username userUsername = Username.of(username);
        Email userEmail = Email.of(email);

        if (userRepository.existsByTenantIdAndUsername(tenantId, userUsername)) {
            throw new DomainException("Já existe um usuário com o username '" + userUsername.value() + "' neste tenant");
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, userEmail)) {
            throw new DomainException("Já existe um usuário com o email '" + userEmail.value() + "' neste tenant");
        }

        User user = User.builder()
                .id(UserId.of(idGenerator.generate()))
                .tenantId(tenantId)
                .username(userUsername)
                .email(userEmail)
                .password(Password.fromPlainText(plainPassword))
                .name(name)
                .build();

        User saved = userRepository.save(user);
        emailSender.sendWelcomeEmail(saved.getEmail().value(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public User updateUser(TenantId tenantId, UserId id, String newName, String newEmail) {
        User user = findByIdOrThrow(tenantId, id);
        Email email = Email.of(newEmail);

        if (!user.getEmail().equals(email) && userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new DomainException("Já existe um usuário com o email '" + email.value() + "' neste tenant");
        }

        user.updateName(newName);
        user.updateEmail(email);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User activateUser(TenantId tenantId, UserId id) {
        User user = findByIdOrThrow(tenantId, id);
        user.activate();
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User blockUser(TenantId tenantId, UserId id) {
        User user = findByIdOrThrow(tenantId, id);
        user.block();
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User disableUser(TenantId tenantId, UserId id) {
        User user = findByIdOrThrow(tenantId, id);
        user.disable();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(TenantId tenantId, UserId id) {
        return findByIdOrThrow(tenantId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsersByTenant(TenantId tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable);
    }

    private User findByIdOrThrow(TenantId tenantId, UserId id) {
        return userRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
}
