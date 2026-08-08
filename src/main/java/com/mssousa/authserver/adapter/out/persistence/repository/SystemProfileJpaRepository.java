package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemProfileJpaRepository extends JpaRepository<SystemProfileEntity, Long> {
    Optional<SystemProfileEntity> findBySystemIdAndId(Long systemId, Long id);
    List<SystemProfileEntity> findBySystemId(Long systemId);
    Optional<SystemProfileEntity> findBySystemIdAndCode(Long systemId, String code);
    void deleteBySystemIdAndId(Long systemId, Long id);
}
