package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SystemTenantRepository {

    SystemTenant save(SystemTenant systemTenant);

    Optional<SystemTenant> findById(SystemTenantId id);

    /**
     * Resolve o vínculo sistema-tenant a partir do sistema — cardinalidade 1:1
     * (decisão D3). Usado na cascata de login (seção 3.4) logo após resolver o sistema
     * pelo client_id.
     */
    Optional<SystemTenant> findBySystemId(SystemId systemId);

    Page<SystemTenant> findByTenantId(TenantId tenantId, Pageable pageable);

    void deleteById(SystemTenantId id);
}
