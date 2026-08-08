package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSystemProfileJpaRepository extends JpaRepository<UserSystemProfileEntity, Long> {
    Optional<UserSystemProfileEntity> findByUserSystemIdAndId(Long userSystemId, Long id);
    List<UserSystemProfileEntity> findByUserSystemId(Long userSystemId);
    Optional<UserSystemProfileEntity> findByUserSystemIdAndSystemProfileId(Long userSystemId, Long systemProfileId);
    void deleteByUserSystemIdAndId(Long userSystemId, Long id);
}
