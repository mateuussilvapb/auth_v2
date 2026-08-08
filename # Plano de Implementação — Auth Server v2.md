# Plano de Implementação — Auth Server V2 Multi-Tenant

> **Documento de especificação para implementação.** Autocontido: pode ser entregue a outro agente/desenvolvedor sem contexto adicional.
> **Projeto de referência:** `auth_api` (`com.mssousa:auth`) — os padrões de código descritos na seção 6 foram extraídos dele e são **obrigatórios**.

---

## 1. Contexto e objetivo

### 1.1 O que este sistema é

Um **Authorization Server OAuth2/OIDC multi-tenant** que centraliza a autenticação de múltiplos sistemas satélite. É um projeto-satélite de infraestrutura: ele não tem regra de negócio própria além de identidade, e existe para que os outros sistemas não precisem implementar login.

### 1.2 O que este sistema NÃO é

**Este NÃO é um sistema de autorização.** Esta é a regra mais importante do projeto e ela deve ser respeitada em todas as decisões de design.

O auth server responde apenas: *"quem é este usuário, e quais perfis ele tem neste sistema?"*. Ele **nunca** responde *"este usuário pode executar esta ação?"*.

- O token carrega **códigos de perfil** (`["ADMIN", "OPERADOR"]`), nunca permissões, telas, menus ou ações.
- Cada sistema satélite recebe os perfis e decide sozinho o que cada perfil pode fazer.
- Não existe tabela de permissões, nem de recursos, nem de ações. Se em algum momento a implementação sentir necessidade disso, o requisito foi mal-interpretado.

### 1.3 O projeto de referência e o que muda

O `auth_api` atual tem domínio, persistência e autorização por sistema maduros e testados, mas é **single-tenant**: a identidade é global (`UNIQUE (username)` e `UNIQUE (email)` no servidor inteiro), e `client_system` é um registro de client OAuth2, não um tenant.

O novo projeto **reaproveita integralmente os padrões** (DDD, Value Objects, Builder validante, bindings com status, adapters de persistência, TSID) e adiciona a dimensão de tenant.

---

## 2. Decisões arquiteturais

Todas as decisões abaixo foram tomadas e **não devem ser revisitadas** durante a implementação.

| # | Decisão | Escolha | Justificativa |
|---|---|---|---|
| D1 | Isolamento entre tenants | Banco único, schema único, coluna `tenant_id` | Menor custo de RDS, uma migration só, escala para milhares de tenants |
| D2 | Escopo da identidade | `user.tenant_id` como FK direta e `NOT NULL` | Permite `UNIQUE (tenant_id, email)` no banco e filtro sem JOIN |
| D3 | Cardinalidade sistema↔tenant | 1 sistema : 1 tenant, via binding `system_tenant` com `UNIQUE (system_id)` | O `client_id` resolve tenant + sistema deterministicamente |
| D4 | Fluxo de autenticação | Authorization Code + PKCE (S256), com tela de login hospedada | SSO real entre sistemas do tenant; a senha nunca passa pelos satélites |
| D5 | Arquitetura | Hexagonal (Ports & Adapters) estrita | Requisito explícito; o projeto atual já está a meio caminho |
| D6 | Frontend | Um único projeto Angular (SPA) cobrindo login, consentimento **e** console administrativo; cliente OAuth2 público via PKCE | Todo o frontend do sistema é Angular — nenhuma tela é renderizada server-side (sem Thymeleaf); backend expõe apenas API REST/JSON |
| D7 | Deploy | EC2 única + Docker Compose + Postgres em volume EBS + `pg_dump` diário para S3 | Custo baixo mantendo durabilidade dos dados |
| D8 | Assinatura do token | RS256 com JWKS público | Satélites validam sem segredo compartilhado; HS256 exigiria distribuir a chave |
| D9 | Usuário deus | Tabela `platform_admin` separada de `user` | Mantém `user.tenant_id NOT NULL` — ver 2.1 |

### 2.1 Por que o "usuário deus" fica em tabela separada

O usuário deus (aqui chamado **Platform Admin**) está *acima* de todos os tenants. Se ele fosse uma linha em `user`, `tenant_id` teria de ser `NULL` para ele.

Um `tenant_id` nulo é a origem clássica de vazamento entre tenants: todo filtro `WHERE tenant_id = ?` passa a precisar de um caso especial, e basta um esquecimento para um tenant enxergar dados de outro. Com `platform_admin` separado, a invariante `user.tenant_id IS NOT NULL` é absoluta e verificável pelo banco.

Custo dessa decisão: dois caminhos de autenticação (um para `user`, um para `platform_admin`). É um custo aceitável e localizado na camada de segurança.

### 2.2 Como o login funciona sem Thymeleaf

Diferente do `auth_api` (que renderiza a tela de login server-side com Thymeleaf), aqui **todo** frontend é Angular — incluindo login e consentimento. Isso muda o fluxo descrito na seção 7.1:

1. O browser chega em `GET /oauth2/authorize`. Sem sessão válida, o `AuthenticationEntryPoint` do Spring Security redireciona para a rota pública `/login` do build Angular (servido pelo próprio nginx/auth server como estático), preservando a URL original para retomar depois.
2. A tela de login Angular **não** usa o fluxo OAuth2 para autenticar — ela é uma página pública que envia usuário/senha para um endpoint de autenticação baseado em sessão (`POST /api/auth/login`, cookie `HttpOnly` de sessão do Spring Security), resolvendo o tenant a partir do `client_id` presente na URL original (nunca de input do usuário — ver seção 7.1).
3. Com sessão estabelecida, o browser é redirecionado de volta para `GET /oauth2/authorize`, que agora sucede e segue o fluxo Authorization Code + PKCE normalmente.
4. A tela de consentimento (quando existir client de terceiro) segue o mesmo padrão: rota Angular pública que consome um endpoint de decisão (`POST /api/auth/consent`).

Ou seja: o backend nunca gera HTML. Ele expõe endpoints REST/JSON de autenticação e consentimento (Fase 7) que a SPA Angular consome (Fase 9) — consulte também `adapter/in/web` na seção 5.

### 2.3 Diferença deliberada em relação ao projeto atual

No `auth_api`, as interfaces de repositório vivem em `domain/repository`. Na arquitetura hexagonal canônica, elas são **portas de saída** e pertencem a `application/port/out`.

**Neste projeto elas ficam em `application/port/out`.** É a única quebra intencional de padrão em relação ao projeto de referência, e existe porque a arquitetura hexagonal foi um requisito explícito. Todo o resto dos padrões é preservado.

---

## 3. Modelo de domínio

### 3.1 Hierarquia

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

### 3.2 Agregados e invariantes

| Agregado | Responsabilidade | Invariantes |
|---|---|---|
| `Tenant` | Organização cliente | `code` único global, imutável após criação; status ACTIVE/INACTIVE |
| `PlatformAdmin` | Usuário deus | `username`/`email` únicos globais; nunca vinculado a tenant |
| `System` | Aplicação satélite (client OAuth2) | `clientId` único global e imutável; ≥1 `redirectUri`; secret só para clients confidenciais |
| `SystemProfile` | Perfil dentro de um sistema | `UNIQUE (systemId, code)` — **repetível entre sistemas, proibido no mesmo** |
| `User` | Usuário final | Pertence a exatamente 1 tenant; `UNIQUE (tenantId, username)` e `UNIQUE (tenantId, email)` |
| `SystemTenant` | Vínculo sistema↔tenant | Um sistema pertence a exatamente 1 tenant |
| `UserSystem` | Vínculo usuário↔sistema | **O sistema deve pertencer ao mesmo tenant do usuário** (ver 3.3) |
| `UserSystemProfile` | Vínculo usuário↔perfil | O perfil deve pertencer ao mesmo sistema do `UserSystem` |

### 3.3 A invariante crítica de tenant

> Um `UserSystem` só pode existir se `user.tenant_id` for igual ao tenant do `system`.

Violar isso significa dar a um usuário do tenant A acesso a um sistema do tenant B — a falha mais grave possível num sistema multi-tenant.

Ela é garantida em **duas camadas**:

1. **Domínio** — o domain service `TenantConsistencyValidator` valida antes de qualquer criação de vínculo.
2. **Banco** — via chaves estrangeiras compostas (seção 4.4). Mesmo um `INSERT` manual no banco é rejeitado.

Nunca confie apenas na camada 1.

### 3.4 Status

Reaproveitar os enums do projeto atual, com a mesma semântica:

- `TenantStatus`: `ACTIVE`, `INACTIVE`
- `SystemStatus`: `ACTIVE`, `INACTIVE`
- `UserStatus`: `ACTIVE`, `BLOCKED`, `DISABLED`
- `ProfileStatus`: `ACTIVE`, `INACTIVE`
- `BindingStatus`: `ACTIVE`, `INACTIVE`, `BLOCKED` (para os três bindings)

**Regra de cascata na autenticação:** o acesso só é concedido se **todos** os níveis estiverem ativos — tenant, sistema, vínculo sistema-tenant, usuário, vínculo usuário-sistema, vínculo usuário-perfil e o próprio perfil. Qualquer um inativo interrompe a cadeia.

---

## 4. Modelo de dados

Migrations Flyway em `src/main/resources/db/migration`. IDs gerados pela aplicação via TSID (`BIGINT`, nunca `BIGSERIAL` — ver 6.4).

### 4.1 Tabelas principais

```sql
-- V1__create_tenant_table.sql
CREATE TABLE tenant (
    id          BIGINT       PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(50),
    CONSTRAINT uq_tenant_code UNIQUE (code)
);
CREATE INDEX idx_tenant_status ON tenant(status);

-- V2__create_platform_admin_table.sql
CREATE TABLE platform_admin (
    id            BIGINT       PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(50),
    CONSTRAINT uq_platform_admin_username UNIQUE (username),
    CONSTRAINT uq_platform_admin_email    UNIQUE (email)
);

-- V3__create_system_table.sql
CREATE TABLE system (
    id             BIGINT       PRIMARY KEY,
    client_id      VARCHAR(100) NOT NULL,
    client_secret  VARCHAR(255),               -- NULL para clients públicos (SPA)
    name           VARCHAR(150) NOT NULL,
    public_client  BOOLEAN      NOT NULL DEFAULT TRUE,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP,
    created_by     VARCHAR(50),
    CONSTRAINT uq_system_client_id UNIQUE (client_id)
);
CREATE INDEX idx_system_status ON system(status);

-- V4__create_system_redirect_uri_table.sql
-- PKCE exige múltiplas URIs por client (dev, homolog, prod)
CREATE TABLE system_redirect_uri (
    id         BIGINT       PRIMARY KEY,
    system_id  BIGINT       NOT NULL,
    uri        VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT fk_sru_system FOREIGN KEY (system_id)
        REFERENCES system(id) ON DELETE CASCADE,
    CONSTRAINT uq_sru UNIQUE (system_id, uri)
);

-- V5__create_system_tenant_table.sql
CREATE TABLE system_tenant (
    id         BIGINT      PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    system_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT fk_st_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_system FOREIGN KEY (system_id)
        REFERENCES system(id) ON DELETE CASCADE,
    -- 1 sistema : 1 tenant (decisão D3)
    CONSTRAINT uq_st_system UNIQUE (system_id)
);
CREATE INDEX idx_st_tenant_id ON system_tenant(tenant_id);
-- Necessário para a FK composta de user_system (ver 4.4)
CREATE UNIQUE INDEX uq_st_system_tenant ON system_tenant(system_id, tenant_id);

-- V6__create_user_table.sql
CREATE TABLE "user" (
    id            BIGINT       PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(50),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    -- Identidade escopada ao tenant (decisão D2)
    CONSTRAINT uq_user_username UNIQUE (tenant_id, username),
    CONSTRAINT uq_user_email    UNIQUE (tenant_id, email)
);
CREATE INDEX idx_user_tenant_id ON "user"(tenant_id);
CREATE INDEX idx_user_status    ON "user"(status);
-- Necessário para a FK composta de user_system (ver 4.4)
CREATE UNIQUE INDEX uq_user_id_tenant ON "user"(id, tenant_id);

-- V7__create_system_profile_table.sql
CREATE TABLE system_profile (
    id          BIGINT       PRIMARY KEY,
    system_id   BIGINT       NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(50),
    CONSTRAINT fk_sp_system FOREIGN KEY (system_id)
        REFERENCES system(id) ON DELETE CASCADE,
    -- ADMIN pode existir no sistema A e no B; nunca duas vezes no mesmo
    CONSTRAINT uq_sp_code UNIQUE (system_id, code)
);
CREATE INDEX idx_sp_system_id ON system_profile(system_id);
CREATE INDEX idx_sp_status    ON system_profile(status);
```

### 4.2 Bindings de usuário

```sql
-- V8__create_user_system_table.sql
CREATE TABLE user_system (
    id         BIGINT      PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    system_id  BIGINT      NOT NULL,
    tenant_id  BIGINT      NOT NULL,   -- redundante DE PROPÓSITO (ver 4.4)
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT uq_user_system UNIQUE (user_id, system_id),
    CONSTRAINT fk_us_user_tenant FOREIGN KEY (user_id, tenant_id)
        REFERENCES "user"(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_us_system_tenant FOREIGN KEY (system_id, tenant_id)
        REFERENCES system_tenant(system_id, tenant_id) ON DELETE CASCADE
);
CREATE INDEX idx_us_user_id   ON user_system(user_id);
CREATE INDEX idx_us_system_id ON user_system(system_id);
CREATE INDEX idx_us_status    ON user_system(status);

-- V9__create_user_system_profile_table.sql
CREATE TABLE user_system_profile (
    id                BIGINT      PRIMARY KEY,
    user_system_id    BIGINT      NOT NULL,
    system_profile_id BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(50),
    CONSTRAINT fk_usp_user_system FOREIGN KEY (user_system_id)
        REFERENCES user_system(id) ON DELETE CASCADE,
    CONSTRAINT fk_usp_profile FOREIGN KEY (system_profile_id)
        REFERENCES system_profile(id) ON DELETE CASCADE,
    CONSTRAINT uq_usp UNIQUE (user_system_id, system_profile_id)
);
CREATE INDEX idx_usp_user_system_id ON user_system_profile(user_system_id);
CREATE INDEX idx_usp_profile_id     ON user_system_profile(system_profile_id);
CREATE INDEX idx_usp_status         ON user_system_profile(status);

-- V10__create_password_reset_token_table.sql
CREATE TABLE password_reset_token (
    id         BIGINT       PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,   -- SHA-256 do token, nunca o valor puro
    user_id    BIGINT       NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT uq_prt_token UNIQUE (token_hash),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id)
        REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE INDEX idx_prt_expires_at ON password_reset_token(expires_at);
```

### 4.3 Tabelas do Spring Authorization Server

O fluxo `authorization_code` exige persistir estado de autorização. Usar o schema oficial do Spring Authorization Server, sem modificações:

```
-- V11__create_oauth2_authorization_tables.sql
--   oauth2_authorization
--   oauth2_authorization_consent
```

Copiar o DDL de `org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql` e `oauth2-authorization-consent-schema.sql` do JAR da dependência.

> **Não** usar `oauth2_registered_client`. Os clients vêm da tabela `system` através de um `RegisteredClientRepository` customizado (seção 7.3).

### 4.4 Como as FKs compostas garantem o isolamento

`user_system.tenant_id` é redundante — poderia ser obtido por JOIN. Ele existe para viabilizar as duas FKs compostas:

```
fk_us_user_tenant   → (user_id, tenant_id)   deve existir em user(id, tenant_id)
fk_us_system_tenant → (system_id, tenant_id) deve existir em system_tenant(system_id, tenant_id)
```

Como ambas apontam para a **mesma coluna** `tenant_id` da mesma linha, o banco só aceita o vínculo se usuário e sistema pertencerem ao mesmo tenant. Um `INSERT` cruzando tenants falha com violação de FK, independentemente do que a aplicação tente fazer.

Este é o mecanismo mais importante de segurança do schema. Não removê-lo em refatorações.

---

## 5. Estrutura do projeto (hexagonal)

```
com.mssousa.authserver
│
├── domain/                                    ← ZERO dependências de framework
│   ├── model/
│   │   ├── shared/          DomainId, IdGenerator
│   │   ├── tenant/          Tenant, TenantId, TenantCode, TenantStatus
│   │   ├── platform/        PlatformAdmin, PlatformAdminId, PlatformAdminStatus
│   │   ├── system/          System, SystemId, ClientId, RedirectUri, SystemStatus
│   │   ├── profile/         SystemProfile, SystemProfileId, ProfileCode, ProfileStatus
│   │   ├── user/            User, UserId, Username, Email, Password, UserStatus
│   │   ├── binding/         BindingStatus,
│   │   │                    systemTenant/{SystemTenant, SystemTenantId},
│   │   │                    userSystem/{UserSystem, UserSystemId},
│   │   │                    userSystemProfile/{UserSystemProfile, UserSystemProfileId}
│   │   └── token/           PasswordResetToken, PasswordResetTokenId, ResetTokenValue
│   ├── exception/           DomainException
│   └── service/             AccessValidator, TenantConsistencyValidator,
│                            PlatformAdminPolicy, ProfileUniquenessPolicy
│
├── application/
│   ├── port/
│   │   ├── in/              AuthenticateUserUseCase, AuthorizeUserUseCase,
│   │   │                    ManageTenantUseCase, ManageSystemUseCase,
│   │   │                    ManageUserUseCase, ManageProfileUseCase,
│   │   │                    ManageBindingUseCase, ResetPasswordUseCase
│   │   └── out/             TenantRepository, SystemRepository, UserRepository,
│   │                        SystemProfileRepository, SystemTenantRepository,
│   │                        UserSystemRepository, UserSystemProfileRepository,
│   │                        PlatformAdminRepository, PasswordResetTokenRepository,
│   │                        EmailSenderPort, IdGeneratorPort
│   ├── service/             implementações dos use cases
│   ├── model/               AuthenticatedUser, AuthorizedUser (records de transporte)
│   └── exception/           AuthenticationFailedException, AccessDeniedException
│
├── adapter/
│   ├── in/
│   │   └── web/             controllers REST — admin (/admin/api/**) e auth público
│   │                        (/api/auth/login, /api/auth/consent, /api/auth/forgot-password,
│   │                        branding por tenant), DTOs, mappers. Sem controllers de view:
│   │                        nenhuma tela é renderizada pelo backend (ver decisão D6/2.2)
│   └── out/
│       ├── persistence/
│       │   ├── entity/      *JpaEntity (+ BaseJpaEntity, AuditableJpaEntity)
│       │   ├── repository/  *JpaRepository (Spring Data)
│       │   ├── adapter/     *RepositoryImpl (implementam as portas out)
│       │   └── mapper/      AuthMapper (domínio ↔ entidade)
│       ├── email/           SmtpEmailSender
│       └── id/              TsidGenerator, TsidNodeResolver
│
└── config/                  SecurityConfig, AuthorizationServerConfig,
                             JpaAuditingConfig, CorsConfig, RateLimitConfig,
                             OpenApiConfig
```

### 5.1 Regras de dependência (não negociáveis)

1. `domain` não importa nada de `application`, `adapter`, `config`, Spring ou JPA.
   - Exceção tolerada, herdada do projeto de referência: `BCryptPasswordEncoder` dentro do VO `Password`.
2. `application` importa `domain`. Pode usar anotações Spring (`@Service`, `@Transactional`), mas **nunca** classes web/JPA/OAuth2.
3. `adapter` importa `application` e `domain`.
4. Nenhuma entidade JPA cruza a fronteira de `adapter.out.persistence`. Conversão sempre via `AuthMapper`.
5. Nenhum DTO web entra em `application`. Conversão no controller.

> Recomendação: adicionar o **ArchUnit** e escrever testes que quebrem o build se qualquer regra acima for violada. É o que torna a arquitetura "bem definida" verificável em vez de aspiracional.

---

## 6. Padrões obrigatórios herdados do projeto atual

Estes padrões vêm do `auth_api` e devem ser replicados fielmente.

### 6.1 Value Object

Imutável, `private` constructor, factory `of()`, normalização antes da validação, `equals`/`hashCode` por valor, mensagens de erro como constantes públicas.

```java
public final class TenantCode {
    public static final String ERROR_REQUIRED = "Código do tenant não pode ser nulo ou vazio";
    public static final String ERROR_FORMAT   = "Código deve conter apenas letras minúsculas, números e hífen";

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$");

    private final String value;

    private TenantCode(String value) {
        String normalized = value == null ? null : value.toLowerCase().trim();
        validate(normalized);
        this.value = normalized;
    }

    public static TenantCode of(String value) { return new TenantCode(value); }

    private void validate(String v) {
        if (v == null || v.isBlank())        throw new DomainException(ERROR_REQUIRED);
        if (!PATTERN.matcher(v).matches())   throw new DomainException(ERROR_FORMAT);
    }

    public String value() { return value; }

    @Override public boolean equals(Object o) { /* por valor */ }
    @Override public int hashCode()           { return Objects.hash(value); }
    @Override public String toString()        { return value; }
}
```

`Password` mantém a regra atual: BCrypt com custo **12**, mínimo de 8 caracteres, `fromPlainText()` e `fromHash()`, **sem `toString()`**.

### 6.2 Entidade de domínio

Builder estático com validação **duplicada** (no `build()` e no construtor), campos de identidade `final`, métodos de status idempotentes, mensagens como constantes, Javadoc em português.

```java
public class Tenant {
    public static final String ERROR_ID_REQUIRED   = "ID do tenant não pode ser nulo";
    public static final String ERROR_NAME_REQUIRED = "Nome do tenant não pode ser nulo ou vazio";

    private final TenantId id;
    private final TenantCode code;   // imutável após criação
    private String name;
    private TenantStatus status;

    private Tenant(Builder b) {
        this.id = b.id; this.code = b.code; this.name = b.name; this.status = b.status;
        validate();
    }

    public void activate()   { this.status = TenantStatus.ACTIVE; }
    public void deactivate() { this.status = TenantStatus.INACTIVE; }
    public boolean isActive(){ return this.status == TenantStatus.ACTIVE; }

    public static Builder builder() { return new Builder(); }
    public static class Builder { /* ... build() revalida tudo ... */ }
}
```

### 6.3 Binding

Todo binding segue `UserSystem` do projeto atual: id próprio, dois IDs referenciados, `BindingStatus`, e os métodos `activate()`, `deactivate()`, `block()`, `isActive()`, `validateAccess()`.

### 6.4 Geração de ID

`IdGeneratorPort.generate()` retorna `Long` gerado por TSID na **aplicação**, antes do insert. As tabelas usam `BIGINT PRIMARY KEY` — **nunca** `BIGSERIAL`. `BaseJpaEntity` não usa `@GeneratedValue`.

### 6.5 Adapter de persistência

```java
@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final AuthMapper mapper;

    @Override
    public Optional<User> findByTenantAndUsername(TenantId tenantId, Username username) {
        return jpaRepository.findByTenantIdAndUsername(tenantId.value(), username.value())
                .map(mapper::toDomain);
    }
}
```

> **Regra absoluta:** toda consulta de dado pertencente a tenant recebe `TenantId` como primeiro parâmetro. Um método `findByUsername(Username)` sem tenant é um bug de segurança, não uma conveniência. Ver seção 8.1.

### 6.6 Tratamento de erro e mensagens

- `GlobalExceptionHandler` com `@ControllerAdvice`, no formato do projeto atual (`DomainException` → 422, validação → 400, autenticação → 401, autorização → 403, fallback → 500).
- Mensagens de domínio em **português**; logs e nomes de código em **inglês**.
- Falha de autenticação sempre genérica: `"Invalid credentials"`, sem revelar se o usuário existe, se está bloqueado ou se a senha errou.

---

## 7. Segurança e fluxos

### 7.1 Fluxo de login (Authorization Code + PKCE)

```
1. Satélite gera code_verifier + code_challenge (S256)
2. Browser → GET /oauth2/authorize
       ?response_type=code&client_id=CRM_ACME
       &redirect_uri=...&code_challenge=...&code_challenge_method=S256&state=...
3. Auth server resolve tenant a partir do client_id (via system_tenant)
4. Sem sessão válida, o AuthenticationEntryPoint redireciona para a rota pública
   /login do SPA Angular (preservando a URL original), que resolve o branding do
   tenant a partir do client_id
5. Usuário envia login + senha → POST /api/auth/login (endpoint público, baseado em
   sessão — não é um endpoint OAuth2)
6. AuthenticationService autentica DENTRO do escopo do tenant resolvido; sucesso
   estabelece sessão (cookie HttpOnly) e o Angular redireciona de volta para
   GET /oauth2/authorize, que agora sucede
7. Redirect para redirect_uri com ?code=...&state=...
8. Satélite → POST /oauth2/token (code + code_verifier)
9. JwtTokenCustomizer injeta os claims → retorna access_token + refresh_token
```

**Ponto central:** o tenant vem sempre do `client_id`, nunca de input do usuário. O usuário não escolhe, não digita e não consegue influenciar seu tenant. Ver seção 2.2 para o detalhamento de por que o login é uma SPA Angular pública e não uma tela renderizada pelo backend.

### 7.2 Claims do access token

```json
{
  "iss": "https://auth.seudominio.com",
  "sub": "532847592847592",
  "aud": "CRM_ACME",
  "exp": 1735689600,
  "iat": 1735686000,
  "jti": "b3f1...",
  "tenant_id": "918273645",
  "tenant_code": "acme",
  "client_id": "CRM_ACME",
  "username": "joao.silva",
  "email": "joao@acme.com",
  "name": "João da Silva",
  "profiles": ["ADMIN", "FINANCEIRO"]
}
```

`profiles` contém **códigos de perfil e nada mais**. Sem permissões, sem menus, sem ações. Cada satélite faz o mapeamento perfil → permissões do seu lado.

### 7.3 Configuração do Authorization Server

- `RegisteredClientRepository` **customizado**, lendo de `system` + `system_redirect_uri`. Não usar `JdbcRegisteredClientRepository`.
- `OAuth2AuthorizationService`: `JdbcOAuth2AuthorizationService` (estado precisa sobreviver a restart).
- PKCE **obrigatório** (`requireProofKey(true)`); `code_challenge_method=plain` rejeitado.
- Chaves RSA 2048 bits, expostas em `/oauth2/jwks`. Rotação com dois `kid` ativos durante a transição.
- Access token: 15 min. Refresh token: 8 h, **com rotação** a cada uso.
- Client secret armazenado com BCrypt; clients públicos (SPA Angular) sem secret.
- Nenhum `ViewResolver`/Thymeleaf no `SecurityConfig` — apenas `AuthenticationEntryPoint`
  redirecionando para a rota `/login` do build Angular, e endpoints REST/JSON de
  autenticação e consentimento (ver 2.2 e `adapter/in/web` na seção 5).

### 7.4 Checklist de segurança

- [ ] BCrypt custo 12 para senha de usuário e de platform admin
- [ ] Token de reset armazenado como hash SHA-256, TTL de 30 min, uso único
- [ ] Rate limiting (Bucket4j) em `/login`, `/oauth2/token` e `/admin/api/**`
- [ ] Bloqueio de conta após N tentativas falhas (contador + janela)
- [ ] `Password` sem `toString()`; nunca logar senha, token ou secret
- [ ] HTTPS obrigatório, HSTS, cookies `Secure` + `HttpOnly` + `SameSite=Lax`
- [ ] CORS restrito às origens dos satélites cadastrados
- [ ] Chaves e segredos fora do repositório (env var / AWS Secrets Manager)
- [ ] Nenhuma credencial em `application*.yml` versionado — **corrigir o vício do projeto atual**, que versiona senha de SMTP e de banco em texto plano
- [ ] `state` validado no fluxo de autorização (proteção CSRF)
- [ ] Auditoria de operações administrativas (quem criou/alterou tenant, sistema, usuário, perfil)

---

## 8. Isolamento entre tenants (a parte que não pode falhar)

### 8.1 Três camadas de defesa

| Camada | Mecanismo | Garante |
|---|---|---|
| Banco | FKs compostas (4.4) + `UNIQUE (tenant_id, ...)` | Vínculo cruzando tenants é impossível de inserir |
| Repositório | Toda porta recebe `TenantId` explícito | Nenhuma consulta "esquece" o escopo |
| Domínio | `TenantConsistencyValidator` | Erro de negócio claro antes de chegar ao banco |

### 8.2 Alternativa considerada e rejeitada

Filtro global do Hibernate (`@Filter` / `@TenantId`) com um `TenantContext` em `ThreadLocal` reduz boilerplate, mas falha silenciosamente quando o contexto não é preenchido — em jobs assíncronos, `@Async`, listeners e testes. Como a falha é silenciosa e o impacto é vazamento entre tenants, **o parâmetro explícito foi preferido**: verboso, porém impossível de esquecer sem quebrar a compilação.

### 8.3 Testes obrigatórios de isolamento

Suite dedicada, que deve existir desde a Fase 3:

- [ ] Usuário do tenant A não autentica em sistema do tenant B
- [ ] `UNIQUE (tenant_id, email)` permite o mesmo e-mail em tenants distintos
- [ ] Criar `UserSystem` cruzando tenants falha na FK do banco (teste com SQL direto, não só via serviço)
- [ ] Token emitido para o client do tenant A nunca carrega `tenant_id` do tenant B
- [ ] Perfil do sistema X não pode ser vinculado a usuário de sistema Y
- [ ] Tenant inativo impede login em todos os seus sistemas
- [ ] Platform admin acessa todos os tenants; usuário comum, apenas o seu

---

## 9. API administrativa (usuário deus)

Base: `/admin/api/v1`. Todos os endpoints exigem token de **platform admin**.

| Recurso | Endpoints |
|---|---|
| Tenants | `POST /tenants` · `GET /tenants` · `GET /tenants/{id}` · `PUT /tenants/{id}` · `PATCH /tenants/{id}/status` |
| Sistemas | `POST /tenants/{tenantId}/systems` · `GET /tenants/{tenantId}/systems` · `PUT /systems/{id}` · `PATCH /systems/{id}/status` · `POST /systems/{id}/redirect-uris` · `DELETE /systems/{id}/redirect-uris/{uriId}` · `POST /systems/{id}/rotate-secret` |
| Perfis | `POST /systems/{systemId}/profiles` · `GET /systems/{systemId}/profiles` · `PUT /profiles/{id}` · `PATCH /profiles/{id}/status` |
| Usuários | `POST /tenants/{tenantId}/users` · `GET /tenants/{tenantId}/users` · `PUT /users/{id}` · `PATCH /users/{id}/status` · `POST /users/{id}/reset-password` |
| Vínculos | `POST /users/{userId}/systems` · `PATCH /user-systems/{id}/status` · `POST /user-systems/{id}/profiles` · `PATCH /user-system-profiles/{id}/status` |
| Platform admins | `POST /platform-admins` · `GET /platform-admins` · `PATCH /platform-admins/{id}/status` |

Paginação com `Pageable` (padrão do `UserService` atual). Documentação via springdoc-openapi.

---

## 10. Fases de implementação

Ordem bottom-up, espelhando a evolução do projeto de referência. Cada fase deve estar **verde nos testes** antes da seguinte.

### Fase 0 — Fundação
- [ ] Projeto Spring Boot 4.x, Java 25, Maven (copiar `pom.xml` de referência)
- [ ] Remover as dependências `jjwt-*` — resíduo inútil no projeto atual, o Spring Authorization Server já assina o token
- [ ] Dependências: web, data-jpa, security, oauth2-authorization-server, validation, flyway, postgresql, mail, actuator, lombok, hypersistence-tsid, bucket4j, springdoc-openapi (sem thymeleaf — frontend é 100% Angular, ver decisão D6)
- [ ] Testes: testcontainers, archunit, spring-security-test
- [ ] `.gitignore` cobrindo `*.pem`, `*.key`, `.env`, `application-local.yml`
- [ ] `docker-compose.yml` de desenvolvimento (Postgres + MailHog)
- [ ] `README.md` com objetivo, modelo de dados e como subir

### Fase 1 — Domínio: núcleo
- [ ] `shared/`: `DomainId`, `IdGenerator`
- [ ] `exception/DomainException`
- [ ] VOs: `TenantCode`, `Username`, `Email`, `Password`, `ClientId`, `ProfileCode`, `RedirectUri`
- [ ] Enums de status (todos)
- [ ] Entidades: `Tenant`, `PlatformAdmin`, `System`, `SystemProfile`, `User`
- [ ] Testes unitários de cada VO e entidade (feliz + todas as violações de invariante)

### Fase 2 — Domínio: bindings e serviços
- [ ] `SystemTenant`, `UserSystem`, `UserSystemProfile` (+ IDs)
- [ ] `TenantConsistencyValidator`
- [ ] `AccessValidator` — cascata completa de status (3.4)
- [ ] `PlatformAdminPolicy`, `ProfileUniquenessPolicy`
- [ ] `PasswordResetToken`, `ResetTokenValue`
- [ ] Testes unitários de todos os serviços de domínio

### Fase 3 — Persistência
- [ ] Migrations V1–V11 (seção 4)
- [ ] `BaseJpaEntity`, `AuditableJpaEntity`
- [ ] Entidades JPA de todas as tabelas
- [ ] `*JpaRepository` (Spring Data)
- [ ] `AuthMapper` — domínio ↔ entidade, para todos os agregados
- [ ] `*RepositoryImpl` implementando as portas out
- [ ] `TsidGenerator`, `TsidNodeResolver`
- [ ] Testes de integração com Testcontainers
- [ ] **Testes de isolamento da seção 8.3** (incluindo violação via SQL direto)

### Fase 4 — Aplicação: administração
- [ ] Use cases de tenant, sistema, perfil, usuário e vínculos
- [ ] Validação de unicidade de perfil por sistema
- [ ] Validação de consistência de tenant em todo vínculo
- [ ] Envio de e-mail de boas-vindas e de reset (porta `EmailSenderPort`)
- [ ] Testes unitários com Mockito

### Fase 5 — Aplicação: autenticação
- [ ] `AuthenticateUserUseCase` — resolve por username **ou** e-mail, dentro do tenant
- [ ] `AuthorizeUserUseCase` — retorna os códigos de perfil ativos
- [ ] `AuthenticatePlatformAdminUseCase`
- [ ] `ResetPasswordUseCase`
- [ ] Falha sempre genérica; sem vazar existência de usuário
- [ ] Testes cobrindo cada nível da cascata de status

### Fase 6 — Segurança e OAuth2
- [ ] `SecurityConfig` — filter chains separados para `/oauth2/**`, `/admin/api/**` e `/api/auth/**` (público, consumido pelo SPA Angular)
- [ ] `AuthorizationServerConfig` — settings, PKCE obrigatório, TTLs
- [ ] `RegisteredClientRepository` customizado sobre `system`
- [ ] `JdbcOAuth2AuthorizationService`
- [ ] Geração/carregamento de chave RSA + `JWKSource` + endpoint JWKS
- [ ] `CustomAuthenticationProvider` (usuário) e provider de platform admin
- [ ] `JwtTokenCustomizer` — claims da seção 7.2
- [ ] Rate limiting e bloqueio por tentativas
- [ ] Testes de emissão e validação de token

### Fase 7 — API de autenticação e consentimento (backend, consumida pelo Angular)
- [ ] `POST /api/auth/login` — autenticação baseada em sessão (não é endpoint OAuth2), dentro do tenant resolvido pelo `client_id` (ver 2.2)
- [ ] Endpoint público de branding por tenant (nome/logo resolvidos pelo `client_id`)
- [ ] Fluxo de "esqueci minha senha" (`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`)
- [ ] `POST /api/auth/consent` — decisão de consentimento (se houver client de terceiro)
- [ ] Mensagens de erro genéricas na API (sem vazar existência de usuário)
- [ ] Testes com MockMvc/`@WebMvcTest`

### Fase 8 — API administrativa
- [ ] Controllers da seção 9
- [ ] DTOs de request/response com Bean Validation
- [ ] `GlobalExceptionHandler`
- [ ] OpenAPI/Swagger
- [ ] CORS para o SPA Angular
- [ ] Testes de integração dos endpoints

### Fase 9 — Frontend Angular (login, consentimento e console administrativo)
- [ ] Projeto Angular único: rotas públicas (`/login`, `/consent`, `/esqueci-senha`) consumindo a API da Fase 7, e rotas protegidas do console consumindo a API da Fase 8
- [ ] Cliente OAuth2 PKCE (`angular-oauth2-oidc`) para o console administrativo
- [ ] Tela de login com branding por tenant e mensagens de erro genéricas
- [ ] CRUD de tenants, sistemas, perfis, usuários
- [ ] Tela de vínculos (usuário → sistemas → perfis)
- [ ] Guard de rota exigindo platform admin nas rotas do console
- [ ] Build de produção servido por nginx (mesmo domínio do auth server)

### Fase 10 — Qualidade
- [ ] Testes ArchUnit das regras da seção 5.1
- [ ] Cobertura ≥ 80% no domínio e na aplicação (JaCoCo)
- [ ] Teste end-to-end: criar tenant → sistema → perfis → usuário → vincular → login → validar claims
- [ ] Seed inicial: primeiro platform admin via migration com senha temporária forçando troca

### Fase 11 — Deploy AWS
- [ ] `Dockerfile` multi-stage (build Maven → runtime JRE slim)
- [ ] `docker-compose.prod.yml`: auth-server + postgres (**volume EBS**) + nginx
- [ ] EC2 t4g.small, EBS separado montado para os dados do Postgres
- [ ] Nginx: TLS (Let's Encrypt), reverse proxy, servindo o Angular
- [ ] Script de `pg_dump` diário → S3 (versioning + lifecycle de 30 dias)
- [ ] **Teste de restore** — backup não testado não é backup
- [ ] Segredos por env var, fora da imagem e do repositório
- [ ] CloudWatch Agent para logs e métricas
- [ ] GitHub Actions: build → test → imagem → deploy
- [ ] Health checks do Actuator + alarme de indisponibilidade

---

## 11. Infraestrutura AWS

```
Route53 → EC2 t4g.small (Elastic IP)
            │
            ├── nginx  (TLS, reverse proxy, estáticos do Angular — login, consent e console)
            ├── auth-server  (Spring Boot, API REST/OAuth2 — sem view server-side)
            └── postgres     → volume EBS gp3 20GB  ← DADOS PERSISTEM AQUI
                                    │
                              pg_dump diário → S3 (versioning + lifecycle)
```

**Custo estimado:** EC2 t4g.small ~US$ 12 + EBS 20GB ~US$ 1,60 + S3 ~US$ 0,50 → **~US$ 15/mês**.

### 11.1 Regras invioláveis do deploy

1. **Os dados do Postgres nunca ficam na camada do container.** Sempre em volume EBS montado. Um `docker compose down` não pode destruir a base.
2. **Backup testado.** Um restore completo deve ser executado e validado ao menos uma vez antes do primeiro uso em produção.
3. **Segredos por variável de ambiente**, jamais na imagem ou no repositório.

### 11.2 Limitações aceitas conscientemente

- **Sem alta disponibilidade.** Instância única: manutenção ou falha da EC2 derruba a autenticação de todos os satélites.
- **RPO de até 24 h** para restores a partir do S3 (o volume EBS cobre os casos de falha do container, que são os frequentes).
- **Escala vertical apenas.** BCrypt custo 12 é intencionalmente pesado em CPU; sob carga real, o caminho é aumentar a instância.

**Gatilhos para migrar para ECS Fargate + RDS:** mais de ~50 tenants ativos, exigência contratual de SLA, ou latência de login acima de 500 ms no percentil 95.

---

## 12. Armadilhas conhecidas

| Armadilha | Prevenção |
|---|---|
| Consulta sem escopo de tenant | Toda porta out recebe `TenantId`; revisar em code review |
| `tenant_id` nulo abrindo brecha | `platform_admin` em tabela separada (D9) |
| Perfil duplicado no mesmo sistema | `UNIQUE (system_id, code)` no banco, não só na aplicação |
| Vínculo cruzando tenants | FKs compostas (4.4) |
| Token revelando existência de usuário | Erro genérico sempre, com tempo de resposta constante |
| Deploy destruindo a base | Volume EBS, nunca a camada do container |
| Confundir autenticação com autorização | Nenhuma tabela de permissão; o token só carrega códigos de perfil |
| Chave RSA perdida no redeploy | Chave em volume persistente ou Secrets Manager, nunca gerada em memória |
| Credenciais versionadas | Corrigir o vício do projeto atual; `.gitignore` + Secrets Manager |
| Migration destrutiva em produção | Flyway com `validate-on-migrate`, sem `clean` habilitado |

---

## 13. Glossário

| Termo | Significado |
|---|---|
| **Tenant** | Organização cliente. Fronteira de isolamento de dados. |
| **System** | Aplicação satélite que delega autenticação. É o client OAuth2. |
| **Profile** (`SystemProfile`) | Perfil dentro de um sistema. Único por sistema, repetível entre sistemas. Chamado `SystemRole` no projeto atual. |
| **Platform Admin** | O "usuário deus". Opera acima dos tenants. |
| **User** | Usuário final, pertencente a exatamente um tenant. |
| **Binding** | Vínculo com status próprio: `SystemTenant`, `UserSystem`, `UserSystemProfile`. |

---

## 14. Como verificar que está pronto

Executar o cenário completo, nesta ordem:

1. Subir o `docker-compose` de desenvolvimento; Flyway aplica V1–V11 sem erro.
2. `mvn verify` — unitários, integração (Testcontainers) e ArchUnit verdes.
3. Autenticar como platform admin no console Angular.
4. Criar tenant `acme`; criar sistema `CRM_ACME`; criar os perfis `ADMIN` e `FINANCEIRO`.
5. Criar tenant `globex`; criar sistema `CRM_GLOBEX`; criar o perfil `ADMIN` — **deve funcionar**, provando que perfis se repetem entre sistemas.
6. Criar `joao@acme.com` no tenant `acme` e `joao@acme.com` no tenant `globex` — **ambos devem funcionar**, provando o multi-tenancy.
7. Vincular o usuário de `acme` ao `CRM_ACME` com o perfil `ADMIN`.
8. Tentar vincular o usuário de `acme` ao `CRM_GLOBEX` — **deve falhar**.
9. Fazer login via PKCE no `CRM_ACME`; decodificar o token e conferir `tenant_id`, `client_id` e `profiles: ["ADMIN"]`.
10. Validar a assinatura do token contra `/oauth2/jwks`.
11. Desativar o tenant `acme`; tentar login — **deve falhar**.
12. Executar o `pg_dump`, destruir o container do Postgres, recriar e restaurar — os dados voltam.
