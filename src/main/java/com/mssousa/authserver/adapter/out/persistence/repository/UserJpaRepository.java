package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByTenantIdAndId(Long tenantId, Long id);
    Optional<UserEntity> findByTenantIdAndUsername(Long tenantId, String username);
    Optional<UserEntity> findByTenantIdAndEmail(Long tenantId, String email);
    boolean existsByTenantIdAndUsername(Long tenantId, String username);
    boolean existsByTenantIdAndEmail(Long tenantId, String email);
    Page<UserEntity> findByTenantId(Long tenantId, Pageable pageable);
    void deleteByTenantIdAndId(Long tenantId, Long id);
}
