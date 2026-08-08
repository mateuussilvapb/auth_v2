package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;

/**
 * Porta de entrada para os vínculos usuário-sistema e usuário-perfil (seção 9:
 * {@code /admin/api/v1/users/{userId}/systems}, {@code /admin/api/v1/user-systems/{id}},
 * {@code /admin/api/v1/user-systems/{id}/profiles}, {@code /admin/api/v1/user-system-profiles/{id}}).
 * <p>
 * Todo vínculo criado aqui passa pelas validações de consistência da seção 3.3 (mesmo
 * tenant) e 3.2 (perfil pertence ao mesmo sistema do vínculo).
 * </p>
 */
public interface ManageBindingUseCase {

    UserSystem bindUserToSystem(TenantId tenantId, UserId userId, SystemId systemId);

    UserSystem activateUserSystem(TenantId tenantId, UserSystemId id);

    UserSystem deactivateUserSystem(TenantId tenantId, UserSystemId id);

    UserSystem blockUserSystem(TenantId tenantId, UserSystemId id);

    UserSystemProfile bindProfileToUserSystem(TenantId tenantId, UserSystemId userSystemId, SystemProfileId profileId);

    UserSystemProfile activateUserSystemProfile(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id);

    UserSystemProfile deactivateUserSystemProfile(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id);

    UserSystemProfile blockUserSystemProfile(TenantId tenantId, UserSystemId userSystemId, UserSystemProfileId id);
}
