package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PlatformAdminRepository {

    PlatformAdmin save(PlatformAdmin platformAdmin);

    Optional<PlatformAdmin> findById(PlatformAdminId id);

    Optional<PlatformAdmin> findByUsername(Username username);

    Optional<PlatformAdmin> findByEmail(Email email);

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    /**
     * Usado pelo {@code PlatformAdminPolicy} para impedir desativar o último ativo.
     */
    long countByStatus(PlatformAdminStatus status);

    Page<PlatformAdmin> findAll(Pageable pageable);

    void deleteById(PlatformAdminId id);
}
