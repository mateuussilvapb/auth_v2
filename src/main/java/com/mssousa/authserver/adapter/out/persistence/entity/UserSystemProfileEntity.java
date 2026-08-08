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
@Table(name = "user_system_profile")
@Getter
@Setter
@NoArgsConstructor
public class UserSystemProfileEntity extends AuditableJpaEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_system_id", nullable = false)
    private UserSystemEntity userSystem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "system_profile_id", nullable = false)
    private SystemProfileEntity systemProfile;

    @Column(nullable = false, length = 20)
    private String status;
}
