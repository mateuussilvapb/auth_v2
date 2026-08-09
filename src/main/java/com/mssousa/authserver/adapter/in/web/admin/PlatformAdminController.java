package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.adapter.in.web.common.UpdateStatusRequest;
import com.mssousa.authserver.application.port.in.ManagePlatformAdminUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /admin/api/v1/platform-admins} (seção 9 do plano) — protegido por
 * {@code SecurityConfig.adminApiSecurityFilterChain}, exige token de platform admin (um
 * platform admin gerencia os demais).
 */
@RestController
@RequestMapping("/admin/api/v1/platform-admins")
public class PlatformAdminController {

    private final ManagePlatformAdminUseCase managePlatformAdminUseCase;

    public PlatformAdminController(ManagePlatformAdminUseCase managePlatformAdminUseCase) {
        this.managePlatformAdminUseCase = managePlatformAdminUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformAdminResponse create(@Valid @RequestBody CreatePlatformAdminRequest request) {
        PlatformAdmin admin = managePlatformAdminUseCase.createPlatformAdmin(
                request.username(), request.email(), request.password(), request.name());
        return PlatformAdminResponse.from(admin);
    }

    @GetMapping
    public Page<PlatformAdminResponse> list(Pageable pageable) {
        return managePlatformAdminUseCase.listPlatformAdmins(pageable).map(PlatformAdminResponse::from);
    }

    @PatchMapping("/{id}/status")
    public PlatformAdminResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        PlatformAdminStatus status = parseStatus(request.status());
        PlatformAdmin admin = status == PlatformAdminStatus.ACTIVE
                ? managePlatformAdminUseCase.activatePlatformAdmin(PlatformAdminId.of(id))
                : managePlatformAdminUseCase.deactivatePlatformAdmin(PlatformAdminId.of(id));
        return PlatformAdminResponse.from(admin);
    }

    private PlatformAdminStatus parseStatus(String status) {
        try {
            return PlatformAdminStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Status inválido para platform admin: '" + status + "'");
        }
    }
}
