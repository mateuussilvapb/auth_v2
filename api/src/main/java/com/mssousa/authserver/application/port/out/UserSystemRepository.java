package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * TenantId explícito em toda consulta (seção 6.5), mesmo sendo redundante com o que já
 * está embutido no próprio vínculo (seção 4.4) — reforça a regra em vez de contornar.
 */
public interface UserSystemRepository {

    UserSystem save(UserSystem userSystem);

    Optional<UserSystem> findByTenantIdAndId(TenantId tenantId, UserSystemId id);

    Optional<UserSystem> findByTenantIdAndUserIdAndSystemId(TenantId tenantId, UserId userId, SystemId systemId);

    Page<UserSystem> findByTenantIdAndUserId(TenantId tenantId, UserId userId, Pageable pageable);

    void deleteByTenantIdAndId(TenantId tenantId, UserSystemId id);
}
