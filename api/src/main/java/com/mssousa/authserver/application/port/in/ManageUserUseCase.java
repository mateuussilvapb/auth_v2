package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Porta de entrada para administração de usuários (seção 9:
 * {@code /admin/api/v1/tenants/{tenantId}/users}, {@code /admin/api/v1/users/{id}}).
 */
public interface ManageUserUseCase {

    User createUser(TenantId tenantId, String username, String email, String plainPassword, String name);

    User updateUser(TenantId tenantId, UserId id, String newName, String newEmail);

    User activateUser(TenantId tenantId, UserId id);

    User blockUser(TenantId tenantId, UserId id);

    User disableUser(TenantId tenantId, UserId id);

    User getUser(TenantId tenantId, UserId id);

    Page<User> listUsersByTenant(TenantId tenantId, Pageable pageable);
}
