package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.PlatformAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformAdminJpaRepository extends JpaRepository<PlatformAdminEntity, Long> {
    Optional<PlatformAdminEntity> findByUsername(String username);
    Optional<PlatformAdminEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByStatus(String status);
}
