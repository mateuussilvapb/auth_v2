package com.mssousa.authserver.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * Superclasse para as entidades que também rastreiam {@code updated_at}: tenant,
 * platform_admin, system e user (seção 4.1 do plano). Os demais bindings e o perfil só
 * têm {@code created_at}.
 */
@MappedSuperclass
public abstract class UpdatableJpaEntity extends AuditableJpaEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
