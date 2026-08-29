package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.adapter.in.web.common.UpdateStatusRequest;
import com.mssousa.authserver.application.port.in.ManageSystemUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.system.SystemStatus;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /admin/api/v1/tenants/{tenantId}/systems} e {@code /admin/api/v1/systems}
 * (seção 9 do plano) — protegido por {@code SecurityConfig.adminApiSecurityFilterChain}.
 * <p>
 * O item "{@code DELETE /systems/{id}/redirect-uris/{uriId}}" da seção 9 pressupõe um ID
 * de redirect URI que não existe no domínio ({@link com.mssousa.authserver.domain.model.system.RedirectUri}
 * é um Value Object sem identidade, seção 6.1) — a remoção usa a própria URI como
 * identificador (query param), não um ID sintético.
 * </p>
 */
@RestController
public class SystemController {

    private final ManageSystemUseCase manageSystemUseCase;

    public SystemController(ManageSystemUseCase manageSystemUseCase) {
        this.manageSystemUseCase = manageSystemUseCase;
    }

    @PostMapping("/admin/api/v1/tenants/{tenantId}/systems")
    @ResponseStatus(HttpStatus.CREATED)
    public SystemResponse create(@PathVariable Long tenantId, @Valid @RequestBody CreateSystemRequest request) {
        System system = manageSystemUseCase.createSystem(TenantId.of(tenantId), request.clientId(), request.name(),
                request.publicClient(), request.clientSecret(), request.initialRedirectUris(), request.thirdParty());
        return SystemResponse.from(system);
    }

    @GetMapping("/admin/api/v1/tenants/{tenantId}/systems")
    public Page<SystemResponse> listByTenant(@PathVariable Long tenantId, Pageable pageable) {
        return manageSystemUseCase.listSystemsByTenant(TenantId.of(tenantId), pageable).map(SystemResponse::from);
    }

    @PutMapping("/admin/api/v1/systems/{id}")
    public SystemResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSystemRequest request) {
        return SystemResponse.from(manageSystemUseCase.updateSystem(SystemId.of(id), request.name()));
    }

    @PatchMapping("/admin/api/v1/systems/{id}/status")
    public SystemResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        SystemStatus status = parseStatus(request.status());
        System system = status == SystemStatus.ACTIVE
                ? manageSystemUseCase.activateSystem(SystemId.of(id))
                : manageSystemUseCase.deactivateSystem(SystemId.of(id));
        return SystemResponse.from(system);
    }

    @PostMapping("/admin/api/v1/systems/{id}/redirect-uris")
    @ResponseStatus(HttpStatus.CREATED)
    public SystemResponse addRedirectUri(@PathVariable Long id, @Valid @RequestBody RedirectUriRequest request) {
        return SystemResponse.from(manageSystemUseCase.addRedirectUri(SystemId.of(id), request.uri()));
    }

    @DeleteMapping("/admin/api/v1/systems/{id}/redirect-uris")
    public SystemResponse removeRedirectUri(@PathVariable Long id, @RequestParam String uri) {
        return SystemResponse.from(manageSystemUseCase.removeRedirectUri(SystemId.of(id), uri));
    }

    @PostMapping("/admin/api/v1/systems/{id}/rotate-secret")
    public SystemResponse rotateSecret(@PathVariable Long id, @Valid @RequestBody RotateSecretRequest request) {
        return SystemResponse.from(manageSystemUseCase.rotateSecret(SystemId.of(id), request.newSecret()));
    }

    private SystemStatus parseStatus(String status) {
        try {
            return SystemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Status inválido para sistema: '" + status + "'");
        }
    }
}
