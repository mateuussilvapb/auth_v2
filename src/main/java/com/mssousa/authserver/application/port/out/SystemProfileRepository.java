package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;

import java.util.List;
import java.util.Optional;

/**
 * Perfil não tem coluna de tenant própria (pertence a um sistema, seção 3.2) — toda
 * consulta recebe {@link SystemId} explícito como escopo, análogo à regra de
 * {@link UserRepository} para tenant.
 */
public interface SystemProfileRepository {

    SystemProfile save(SystemProfile profile);

    Optional<SystemProfile> findBySystemIdAndId(SystemId systemId, SystemProfileId id);

    List<SystemProfile> findBySystemId(SystemId systemId);

    Optional<SystemProfile> findBySystemIdAndCode(SystemId systemId, ProfileCode code);

    void deleteBySystemIdAndId(SystemId systemId, SystemProfileId id);
}
