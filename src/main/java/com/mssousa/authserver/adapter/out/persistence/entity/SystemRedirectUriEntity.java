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
@Table(name = "system_redirect_uri")
@Getter
@Setter
@NoArgsConstructor
public class SystemRedirectUriEntity extends AuditableJpaEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemEntity system;

    @Column(nullable = false, length = 500)
    private String uri;
}
