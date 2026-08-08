# Auth Server V2

Authorization Server OAuth2/OIDC **multi-tenant**, responsável apenas por autenticação
(*"quem é este usuário, e quais perfis ele tem neste sistema?"*). Não é um sistema de
autorização: o token carrega apenas códigos de perfil (`["ADMIN", "OPERADOR"]`); cada
sistema satélite decide o que cada perfil pode fazer.

Ver `# Plano de Implementação — Auth Server v2.md` na raiz do repo para a especificação
completa (arquitetura, decisões, modelo de dados, fases de implementação).

## Modelo de domínio

```
tenant
  │
  └──< system_tenant >── system ──< system_profile
                            │             │
       user ──< user_system >             │
        │            │                    │
        │            └──< user_system_profile >
        │
   (user.tenant_id ─────────────> tenant)
```

- **Tenant** — organização cliente; fronteira de isolamento de dados.
- **System** — aplicação satélite (client OAuth2), vinculada a exatamente 1 tenant.
- **SystemProfile** — perfil dentro de um sistema (único por sistema, repetível entre sistemas).
- **User** — usuário final, pertence a exatamente 1 tenant.
- **PlatformAdmin** — usuário deus, opera acima dos tenants, em tabela separada.

Isolamento entre tenants é a prioridade #1 do projeto, garantido em três camadas: FKs
compostas no banco, `TenantId` explícito em toda porta de repositório, e validação de
domínio (`TenantConsistencyValidator`).

## Stack

Java 25, Spring Boot 4.x, Maven, PostgreSQL, Flyway, Spring Authorization Server,
arquitetura hexagonal (Ports & Adapters).

## Como subir o ambiente de desenvolvimento

```bash
docker compose up -d
./mvnw spring-boot:run
```

O `docker-compose.yml` sobe Postgres e MailHog (captura de e-mails de dev). Credenciais
de banco/e-mail ficam em variáveis de ambiente — nunca em `application*.yml` versionado.

## Testes

```bash
./mvnw test      # unitários
./mvnw verify     # unitários + integração (Testcontainers) + ArchUnit
```
