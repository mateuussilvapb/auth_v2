package com.mssousa.authserver.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para testes de integração que precisam de um Postgres real (Testcontainers).
 * <p>
 * Container "singleton" iniciado manualmente num bloco estático, de propósito sem as
 * anotações {@code @Testcontainers}/{@code @Container}: essa combinação para em campo
 * {@code static} chama {@code stop()} no {@code afterAll} da PRIMEIRA classe de teste
 * que o referencia, mesmo sendo compartilhado — derrubando o container para todas as
 * classes seguintes. Sem essas anotações, o container só é finalizado pelo Ryuk quando a
 * JVM de teste encerra, permitindo reuso real entre todas as subclasses.
 * </p>
 */
@Tag("integration")
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
