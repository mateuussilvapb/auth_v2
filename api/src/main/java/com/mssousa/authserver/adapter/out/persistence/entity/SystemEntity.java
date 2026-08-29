package com.mssousa.authserver.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "system")
@Getter
@Setter
@NoArgsConstructor
public class SystemEntity extends UpdatableJpaEntity {

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "public_client", nullable = false)
    private boolean publicClient;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "third_party", nullable = false)
    private boolean thirdParty;

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SystemRedirectUriEntity> redirectUris = new ArrayList<>();
}
