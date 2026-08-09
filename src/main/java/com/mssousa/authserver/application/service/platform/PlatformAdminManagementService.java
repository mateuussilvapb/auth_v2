package com.mssousa.authserver.application.service.platform;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.in.ManagePlatformAdminUseCase;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import com.mssousa.authserver.domain.service.PlatformAdminPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformAdminManagementService implements ManagePlatformAdminUseCase {

    private final PlatformAdminRepository platformAdminRepository;
    private final IdGeneratorPort idGenerator;
    private final PlatformAdminPolicy platformAdminPolicy;

    @Override
    @Transactional
    public PlatformAdmin createPlatformAdmin(String username, String email, String plainPassword, String name) {
        Username adminUsername = Username.of(username);
        Email adminEmail = Email.of(email);

        if (platformAdminRepository.existsByUsername(adminUsername)) {
            throw new DomainException("Já existe um platform admin com o username '" + adminUsername.value() + "'");
        }
        if (platformAdminRepository.existsByEmail(adminEmail)) {
            throw new DomainException("Já existe um platform admin com o email '" + adminEmail.value() + "'");
        }

        PlatformAdmin admin = PlatformAdmin.builder()
                .id(PlatformAdminId.of(idGenerator.generate()))
                .username(adminUsername)
                .email(adminEmail)
                .password(Password.fromPlainText(plainPassword))
                .name(name)
                .build();

        return platformAdminRepository.save(admin);
    }

    @Override
    @Transactional
    public PlatformAdmin activatePlatformAdmin(PlatformAdminId id) {
        PlatformAdmin admin = findByIdOrThrow(id);
        admin.activate();
        return platformAdminRepository.save(admin);
    }

    @Override
    @Transactional
    public PlatformAdmin deactivatePlatformAdmin(PlatformAdminId id) {
        PlatformAdmin admin = findByIdOrThrow(id);
        long activeCount = platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE);
        platformAdminPolicy.validateCanDeactivate(admin, activeCount);
        admin.deactivate();
        return platformAdminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlatformAdmin> listPlatformAdmins(Pageable pageable) {
        return platformAdminRepository.findAll(pageable);
    }

    private PlatformAdmin findByIdOrThrow(PlatformAdminId id) {
        return platformAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Platform admin não encontrado: " + id));
    }
}
