package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Porta de entrada para administração de tenants (seção 9: {@code /admin/api/v1/tenants}).
 */
public interface ManageTenantUseCase {

    Tenant createTenant(String code, String name);

    Tenant updateTenant(TenantId id, String newName);

    Tenant activateTenant(TenantId id);

    Tenant deactivateTenant(TenantId id);

    Tenant getTenant(TenantId id);

    Page<Tenant> listTenants(Pageable pageable);
}
