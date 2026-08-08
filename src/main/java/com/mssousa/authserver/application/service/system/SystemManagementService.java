package com.mssousa.authserver.application.service.system;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.port.in.ManageSystemUseCase;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.application.port.out.TenantRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.ClientSecret;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemManagementService implements ManageSystemUseCase {

    private final SystemRepository systemRepository;
    private final SystemTenantRepository systemTenantRepository;
    private final TenantRepository tenantRepository;
    private final IdGeneratorPort idGenerator;

    @Override
    @Transactional
    public System createSystem(TenantId tenantId, String clientId, String name, boolean publicClient,
                                String clientSecret, List<String> initialRedirectUris) {
        if (tenantRepository.findById(tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Tenant não encontrado: " + tenantId);
        }

        ClientId systemClientId = ClientId.of(clientId);
        if (systemRepository.existsByClientId(systemClientId)) {
            throw new DomainException("Já existe um sistema com o client_id '" + systemClientId.value() + "'");
        }

        System.Builder builder = System.builder()
                .id(SystemId.of(idGenerator.generate()))
                .clientId(systemClientId)
                .name(name)
                .publicClient(publicClient)
                .clientSecret(clientSecret == null ? null : ClientSecret.fromPlainText(clientSecret));

        initialRedirectUris.forEach(uri -> builder.redirectUri(RedirectUri.of(uri)));

        System saved = systemRepository.save(builder.build());

        SystemTenant binding = SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenantId)
                .systemId(saved.getId())
                .build();
        systemTenantRepository.save(binding);

        return saved;
    }

    @Override
    @Transactional
    public System updateSystem(SystemId id, String newName) {
        System system = findByIdOrThrow(id);
        system.updateName(newName);
        return systemRepository.save(system);
    }

    @Override
    @Transactional
    public System activateSystem(SystemId id) {
        System system = findByIdOrThrow(id);
        system.activate();
        return systemRepository.save(system);
    }

    @Override
    @Transactional
    public System deactivateSystem(SystemId id) {
        System system = findByIdOrThrow(id);
        system.deactivate();
        return systemRepository.save(system);
    }

    @Override
    @Transactional
    public System addRedirectUri(SystemId id, String uri) {
        System system = findByIdOrThrow(id);
        system.addRedirectUri(RedirectUri.of(uri));
        return systemRepository.save(system);
    }

    @Override
    @Transactional
    public System removeRedirectUri(SystemId id, String uri) {
        System system = findByIdOrThrow(id);
        system.removeRedirectUri(RedirectUri.of(uri));
        return systemRepository.save(system);
    }

    @Override
    @Transactional
    public System rotateSecret(SystemId id, String newSecret) {
        System system = findByIdOrThrow(id);
        system.rotateSecret(ClientSecret.fromPlainText(newSecret));
        return systemRepository.save(system);
    }

    @Override
    @Transactional(readOnly = true)
    public System getSystem(SystemId id) {
        return findByIdOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<System> listSystemsByTenant(TenantId tenantId, Pageable pageable) {
        return systemTenantRepository.findByTenantId(tenantId, pageable)
                .map(binding -> findByIdOrThrow(binding.getSystemId()));
    }

    private System findByIdOrThrow(SystemId id) {
        return systemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sistema não encontrado: " + id));
    }
}
