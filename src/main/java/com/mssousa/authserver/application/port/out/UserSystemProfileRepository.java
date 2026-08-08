package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;

import java.util.List;
import java.util.Optional;

public interface UserSystemProfileRepository {

    UserSystemProfile save(UserSystemProfile userSystemProfile);

    Optional<UserSystemProfile> findByUserSystemIdAndId(UserSystemId userSystemId, UserSystemProfileId id);

    List<UserSystemProfile> findByUserSystemId(UserSystemId userSystemId);

    Optional<UserSystemProfile> findByUserSystemIdAndSystemProfileId(UserSystemId userSystemId, SystemProfileId systemProfileId);

    void deleteByUserSystemIdAndId(UserSystemId userSystemId, UserSystemProfileId id);
}
