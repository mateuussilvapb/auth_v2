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
@Table(name = "system_profile")
@Getter
@Setter
@NoArgsConstructor
public class SystemProfileEntity extends AuditableJpaEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemEntity system;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;
}
