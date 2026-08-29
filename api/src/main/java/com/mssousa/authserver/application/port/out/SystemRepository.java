package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SystemRepository {

    System save(System system);

    Optional<System> findById(SystemId id);

    /**
     * Resolve o sistema a partir do client_id — ponto de entrada do fluxo de login
     * (seção 7.1: o tenant vem sempre do client_id).
     */
    Optional<System> findByClientId(ClientId clientId);

    boolean existsByClientId(ClientId clientId);

    Page<System> findAll(Pageable pageable);

    void deleteById(SystemId id);
}
