package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSystemJpaRepository extends JpaRepository<UserSystemEntity, Long> {
    Optional<UserSystemEntity> findByTenantIdAndId(Long tenantId, Long id);
    Optional<UserSystemEntity> findByTenantIdAndUserIdAndSystemId(Long tenantId, Long userId, Long systemId);
    Page<UserSystemEntity> findByTenantIdAndUserId(Long tenantId, Long userId, Pageable pageable);
    void deleteByTenantIdAndId(Long tenantId, Long id);
}
