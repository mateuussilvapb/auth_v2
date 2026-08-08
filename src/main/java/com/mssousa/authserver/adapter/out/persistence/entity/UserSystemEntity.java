package com.mssousa.authserver.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_system")
@Getter
@Setter
@NoArgsConstructor
public class UserSystemEntity extends AuditableJpaEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemEntity system;

    /**
     * Redundante de propósito (seção 4.4 do plano) — viabiliza as FKs compostas que
     * impedem, mesmo via SQL direto, um vínculo cruzando tenants.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 20)
    private String status;
}
