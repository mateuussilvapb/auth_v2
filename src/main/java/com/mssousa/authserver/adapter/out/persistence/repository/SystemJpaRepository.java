package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemJpaRepository extends JpaRepository<SystemEntity, Long> {
    Optional<SystemEntity> findByClientId(String clientId);
    boolean existsByClientId(String clientId);
}
