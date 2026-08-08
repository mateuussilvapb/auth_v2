package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemTenantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemTenantJpaRepository extends JpaRepository<SystemTenantEntity, Long> {
    Optional<SystemTenantEntity> findBySystemId(Long systemId);
    Page<SystemTenantEntity> findByTenantId(Long tenantId, Pageable pageable);
}
