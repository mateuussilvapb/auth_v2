package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;

import java.util.List;

/**
 * Porta de entrada para administração de perfis de sistema (seção 9:
 * {@code /admin/api/v1/systems/{systemId}/profiles}, {@code /admin/api/v1/profiles/{id}}).
 */
public interface ManageProfileUseCase {

    SystemProfile createProfile(SystemId systemId, String code, String description);

    SystemProfile updateProfileDescription(SystemId systemId, SystemProfileId id, String newDescription);

    SystemProfile activateProfile(SystemId systemId, SystemProfileId id);

    SystemProfile deactivateProfile(SystemId systemId, SystemProfileId id);

    SystemProfile getProfile(SystemId systemId, SystemProfileId id);

    List<SystemProfile> listProfilesBySystem(SystemId systemId);
}
