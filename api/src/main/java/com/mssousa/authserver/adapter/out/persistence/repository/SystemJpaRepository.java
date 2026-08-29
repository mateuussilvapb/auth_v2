package com.mssousa.authserver.adapter.out.persistence.repository;

import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemJpaRepository extends JpaRepository<SystemEntity, Long> {

    /**
     * {@code redirectUris} é {@code @OneToMany} lazy (default do JPA) — {@code AuthMapper}
     * sempre acessa a coleção ao montar o agregado {@code System}, então toda leitura
     * precisa vir com ela já carregada. Sem isso, chamadores fora de uma transação (como
     * {@code SystemRegisteredClientRepository}, invocado direto pelo filter chain do
     * Spring Security, sem `@Transactional` de serviço de aplicação por cima) recebem
     * {@code LazyInitializationException} — não pega em teste porque
     * {@code AbstractRepositoryIntegrationTest} é `@Transactional`, mantendo a sessão
     * Hibernate aberta durante todo o teste.
     */
    @EntityGraph(attributePaths = "redirectUris")
    @Override
    Optional<SystemEntity> findById(Long id);

    @EntityGraph(attributePaths = "redirectUris")
    Optional<SystemEntity> findByClientId(String clientId);

    boolean existsByClientId(String clientId);

    @EntityGraph(attributePaths = "redirectUris")
    @Query("select s from SystemEntity s")
    Page<SystemEntity> findAllWithRedirectUris(Pageable pageable);
}
