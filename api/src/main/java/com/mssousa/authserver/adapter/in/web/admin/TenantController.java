package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.adapter.in.web.common.UpdateStatusRequest;
import com.mssousa.authserver.application.port.in.ManageTenantUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.tenant.TenantStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /admin/api/v1/tenants} (seção 9 do plano) — protegido por
 * {@code config/security/SecurityConfig.adminApiSecurityFilterChain}, exige token de
 * platform admin.
 */
@RestController
@RequestMapping("/admin/api/v1/tenants")
public class TenantController {

    private final ManageTenantUseCase manageTenantUseCase;

    public TenantController(ManageTenantUseCase manageTenantUseCase) {
        this.manageTenantUseCase = manageTenantUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = manageTenantUseCase.createTenant(request.code(), request.name());
        return TenantResponse.from(tenant);
    }

    @GetMapping
    public Page<TenantResponse> list(Pageable pageable) {
        return manageTenantUseCase.listTenants(pageable).map(TenantResponse::from);
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable Long id) {
        return TenantResponse.from(manageTenantUseCase.getTenant(TenantId.of(id)));
    }

    @PutMapping("/{id}")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest request) {
        Tenant tenant = manageTenantUseCase.updateTenant(TenantId.of(id), request.name());
        return TenantResponse.from(tenant);
    }

    @PatchMapping("/{id}/status")
    public TenantResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        TenantStatus status = parseStatus(request.status());
        Tenant tenant = status == TenantStatus.ACTIVE
                ? manageTenantUseCase.activateTenant(TenantId.of(id))
                : manageTenantUseCase.deactivateTenant(TenantId.of(id));
        return TenantResponse.from(tenant);
    }

    private TenantStatus parseStatus(String status) {
        try {
            return TenantStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Status inválido para tenant: '" + status + "'");
        }
    }
}
