package com.mssousa.authserver.adapter.out.persistence.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclasse de todas as entidades JPA. O ID é sempre um TSID gerado na aplicação
 * (seção 6.4 do plano) e atribuído explicitamente antes do insert — nunca
 * {@code @GeneratedValue}.
 */
@MappedSuperclass
public abstract class BaseJpaEntity {

    @Id
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
