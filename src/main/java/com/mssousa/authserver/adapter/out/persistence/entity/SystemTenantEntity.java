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
@Table(name = "system_tenant")
@Getter
@Setter
@NoArgsConstructor
public class SystemTenantEntity extends AuditableJpaEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "system_id", nullable = false, unique = true)
    private SystemEntity system;

    @Column(nullable = false, length = 20)
    private String status;
}
