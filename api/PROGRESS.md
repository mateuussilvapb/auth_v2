# Progresso — Auth Server V2

Espelha os checklists da seção 10 do plano (`# Plano de Implementação — Auth Server v2.md`).
Cada fase deve estar verde nos testes antes da seguinte. Atualizado a cada item fechado.

## Fase 0 — Fundação
- [x] Projeto Spring Boot 4.x, Java 25, Maven (copiar `pom.xml` de referência)
- [x] Remover as dependências `jjwt-*` — resíduo inútil no projeto atual, o Spring Authorization Server já assina o token
- [x] Dependências: web, data-jpa, security, oauth2-authorization-server, validation, flyway, postgresql, mail, actuator, lombok, hypersistence-tsid, bucket4j, springdoc-openapi (sem thymeleaf — frontend é 100% Angular, ver decisão D6)
- [x] Testes: testcontainers, archunit, spring-security-test
- [x] `.gitignore` cobrindo `*.pem`, `*.key`, `.env`, `application-local.yml`
- [x] `docker-compose.yml` de desenvolvimento (Postgres + MailHog)
- [x] `README.md` com objetivo, modelo de dados e como subir

## Fase 1 — Domínio: núcleo
- [x] `shared/`: `DomainId`, `IdGenerator`
- [x] `exception/DomainException`
- [x] VOs: `TenantCode`, `Username`, `Email`, `Password`, `ClientId`, `ProfileCode`, `RedirectUri`
- [x] Enums de status (todos)
- [x] Entidades: `Tenant`, `PlatformAdmin`, `System`, `SystemProfile`, `User`
- [x] Testes unitários de cada VO e entidade (feliz + todas as violações de invariante)

## Fase 2 — Domínio: bindings e serviços
- [x] `SystemTenant`, `UserSystem`, `UserSystemProfile` (+ IDs)
- [x] `TenantConsistencyValidator`
- [x] `AccessValidator` — cascata completa de status (3.4)
- [x] `PlatformAdminPolicy`, `ProfileUniquenessPolicy`
- [x] `PasswordResetToken`, `ResetTokenValue`
- [x] Testes unitários de todos os serviços de domínio

## Fase 3 — Persistência
- [x] Migrations V1–V11 (seção 4)
- [x] `BaseJpaEntity`, `AuditableJpaEntity`
- [x] Entidades JPA de todas as tabelas
- [x] `*JpaRepository` (Spring Data)
- [x] `AuthMapper` — domínio ↔ entidade, para todos os agregados
- [x] `*RepositoryImpl` implementando as portas out
- [x] `TsidGenerator`, `TsidNodeResolver`
- [x] Testes de integração com Testcontainers
- [x] Testes de isolamento da seção 8.3 verificáveis em persistência (UNIQUE
      tenant_id+email em tenants distintos; UserSystem cruzando tenants falha na FK
      composta via SQL direto). Os demais itens de 8.3 (cascata de status completa,
      claim do token, platform admin vs. usuário comum) dependem de autenticação e
      ficam para a Fase 5.

## Fase 4 — Aplicação: administração
- [x] Use cases de tenant, sistema, perfil, usuário e vínculos
- [x] Validação de unicidade de perfil por sistema
- [x] Validação de consistência de tenant em todo vínculo
- [x] Envio de e-mail de boas-vindas e de reset (porta `EmailSenderPort`)
- [x] Testes unitários com Mockito

## Fase 5 — Aplicação: autenticação
- [x] `AuthenticateUserUseCase` — resolve por username **ou** e-mail, dentro do tenant
- [x] `AuthorizeUserUseCase` — retorna os códigos de perfil ativos
- [x] `AuthenticatePlatformAdminUseCase`
- [x] `ResetPasswordUseCase`
- [x] Falha sempre genérica; sem vazar existência de usuário
- [x] Testes cobrindo cada nível da cascata de status

## Fase 6 — Segurança e OAuth2
- [x] `SecurityConfig` — filter chains separados para `/oauth2/**`, `/admin/api/**` e `/api/auth/**` (público, consumido pelo SPA Angular)
- [x] `AuthorizationServerConfig` — settings, PKCE obrigatório, TTLs
- [x] `RegisteredClientRepository` customizado sobre `system`
- [x] `JdbcOAuth2AuthorizationService`
- [x] Geração/carregamento de chave RSA + `JWKSource` + endpoint JWKS
- [x] `CustomAuthenticationProvider` (usuário) e provider de platform admin
- [x] `JwtTokenCustomizer` — claims da seção 7.2
- [x] Rate limiting (`RateLimitFilter`, Bucket4j em memória — login, token, admin/api)
- [ ] Bloqueio por N tentativas de login — adiado para a Fase 7 (ver Notas)
- [x] Testes de emissão e validação de token (fluxo completo Authorization Code + PKCE via MockMvc)

## Fase 7 — API de autenticação e consentimento (backend, consumida pelo Angular)
- [x] `POST /api/auth/login` — autenticação baseada em sessão (não é endpoint OAuth2), dentro do tenant resolvido pelo `client_id` (ver 2.2 do plano)
- [x] Bloqueio por N tentativas de login (adiado da Fase 6 — ver Notas)
- [x] Endpoint público de branding por tenant (nome/logo resolvidos pelo `client_id`)
- [x] Fluxo de "esqueci minha senha" (`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`)
- [ ] `POST /api/auth/consent` — decisão de consentimento (adiado para a Fase 8 — ver Notas)
- [x] Mensagens de erro genéricas na API (sem vazar existência de usuário)
- [x] Testes com MockMvc/`@WebMvcTest`

## Fase 8 — API administrativa
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`, escopado a `adapter.in.web.admin`)
- [x] Flag de "cliente de terceiro" em `System` (`ManageSystemUseCase`) — pré-requisito do consentimento adiado da Fase 7
- [x] `POST /api/auth/consent` + `JdbcOAuth2AuthorizationConsentService` (adiado da Fase 7 — ver Notas)
- Controllers da seção 9 (`/admin/api/v1`, DTOs com Bean Validation, testes de integração — item a item):
  - [x] Tenants (`POST` · `GET` · `GET /{id}` · `PUT /{id}` · `PATCH /{id}/status`)
  - [x] Sistemas (`POST` · `GET` · `PUT /{id}` · `PATCH /{id}/status` · redirect-uris · rotate-secret)
  - [x] Perfis (aninhados sob `/systems/{systemId}/profiles/{id}` — ver Notas)
  - [x] Usuários (aninhados sob `/tenants/{tenantId}/users/{id}`, inclui reset-password administrativo)
  - [x] Vínculos (aninhados sob `/tenants/{tenantId}/...` — ver Notas)
  - [x] Platform admins (`ManagePlatformAdminUseCase` novo — não existia desde a Fase 4, ver Notas)
- [x] OpenAPI/Swagger (só `/admin/api/v1/**`, protegido por platform admin — ver Notas)
- [x] CORS para o SPA Angular (`authserver.cors.allowed-origins`, vazio em produção)
- [x] Corrigir `GlobalExceptionHandler`: `DomainException` deve responder 422, não 400
      (seção 6.6 do plano) — achado da revisão de arquitetura de 2026-08-29, ver Notas

## Fase 9 — Frontend Angular (login, consentimento e console administrativo)
- [x] Projeto Angular único: rotas públicas (`/login`, `/consent`, `/esqueci-senha`, `/reset-password`) consumindo a API da Fase 7, e rota `/console` protegida consumindo a API da Fase 8 (só o dashboard placeholder existe — CRUD é item à parte abaixo)
- [x] Cliente OAuth2 PKCE (`angular-oauth2-oidc`) para o console administrativo — ver Notas (client estático `RegisteredClientRepositoryConfig`, login de platform admin via `/api/auth/login`)
- [x] Tela de login com branding por tenant e mensagens de erro genéricas
- [x] CRUD de tenants, sistemas, perfis, usuários — `AdminApiService`,
      `/console/tenants`, `/console/tenants/:tenantId/systems`,
      `/console/systems/:systemId/profiles`, `/console/tenants/:tenantId/users`
- [x] Tela de vínculos (usuário → sistemas → perfis) —
      `/console/tenants/:tenantId/users/:userId/bindings`, ver Notas (endpoints GET novos
      no backend)
- [x] Guard de rota exigindo platform admin nas rotas do console (`consoleAuthGuard` — token só é emitido para platform admin nesse client, ver Notas)
- [x] Build de produção servido por nginx (mesmo domínio do auth server) — `frontend/nginx.conf`, ver Notas (dois bugs reais só apareceram nesse smoke test)
- [x] **Modernizado em 2026-08 (Angular 19→21, PrimeNG 21, Vitest)** — mesmo escopo
      funcional desta fase, reescrito. Histórico completo, decisões de arquitetura e
      achados de backend descobertos no processo em `web/PROGRESS.md`; ver nota datada
      abaixo.

## Fase 10 — Qualidade
- [x] Testes ArchUnit das regras da seção 5.1. Completadas as duas regras que faltavam
      (achado da auditoria de 2026-08-29, ver nota acima): `application_nao_depende_de_dto_web`
      (regra 5.1.5 — nenhum DTO de `adapter.in.web` entra em `application`, já coberta
      transitivamente por `application_nao_depende_de_adapter` mas agora nomeada
      explicitamente) e `entidade_jpa_nao_cruza_fronteira_de_persistence` (regra 5.1.4 —
      nenhuma classe fora de `adapter.out.persistence` depende de `adapter.out.persistence.entity`).
      Removido `allowEmptyShould(true)` de todas as 6 regras pré-existentes — não é mais
      necessário, todo pacote (`domain`/`application`/`adapter`) já tem código real desde a
      Fase 1; as regras agora falham de verdade se violadas. `mvn test -Dtest=ArchitectureTest`
      verde (8/8 regras).
- [x] Cobertura ≥ 80% no domínio e na aplicação (JaCoCo). Plugin `jacoco-maven-plugin`
      adicionado ao `pom.xml`: `prepare-agent` (instrumentação), `report` (fase `test`) e
      `check` (fase `verify`, gate `LINE` `COVEREDRATIO ≥ 0.80`, escopado via `<includes>`
      a `com/mssousa/authserver/domain/**` e `com/mssousa/authserver/application/**` —
      `adapter`/`config` ficam fora do gate desta fase, cobertos por integração/MockMvc, não
      pelo critério de unidade de 80%). **Achado**: versão inicial `0.8.12` (a mesma travada
      na auditoria de 2026-08-29, ver nota acima) falha com
      `Unsupported class file major version 69` — JaCoCo 0.8.12 não suporta bytecode do
      Java 25. Corrigido usando `0.8.15` (última disponível no Maven Central), que instrumenta
      e analisa Java 25 sem erro. Cobertura real medida (`target/site/jacoco/jacoco.csv`,
      domain+application): **92,52%** (1200/1297 linhas) — folga confortável acima do gate de
      80%, nenhum teste novo precisou ser escrito para fechar este item. `mvn verify` verde
      (suíte completa + ArchUnit + gate JaCoCo).
- [x] Teste end-to-end: criar tenant → sistema → perfis → usuário → vincular → login →
      validar claims. `EndToEndFlowIntegrationTest` (novo) automatiza o roteiro manual da
      seção 14 (passos 3–10, já validado à mão na nota "Roteiro manual da seção 14
      concluído" acima) — usa a **API administrativa real** (`/admin/api/v1/**`, a mesma
      que o console Angular consome), não repositórios diretos: cria tenant, sistema,
      perfil, usuário, vincula usuário↔sistema e usuário↔perfil, autentica
      (`POST /api/auth/login`), completa Authorization Code + PKCE
      (`/oauth2/authorize` → `/oauth2/token`) e decodifica o JWT emitido, validando
      `tenant_id`/`tenant_code`/`client_id`/`username`/`email`/`sub`/`profiles` — a
      assinatura é validada implicitamente por `jwtDecoder.decode()` (mesmo `JWKSource` de
      `/oauth2/jwks`, que também é chamado só para confirmar que o endpoint responde 200).
      Dois testes adicionais no mesmo arquivo, reproduzindo os passos 8 e 11 do roteiro
      (isolamento multi-tenant é a prioridade #1 do projeto, seção 1.2): vínculo
      usuário↔sistema cross-tenant rejeitado (422) e login rejeitado (401) com o tenant
      desativado. 3/3 testes verdes; `mvn verify` (suíte completa + ArchUnit + gate JaCoCo)
      verde.
- [x] Seed inicial: primeiro platform admin via migration com senha temporária forçando
      troca. Migration `V15__seed_first_platform_admin.sql`: coluna nova
      `must_change_password BOOLEAN NOT NULL DEFAULT FALSE` em `platform_admin` + `INSERT`
      do primeiro admin (`admin`/`admin@example.com`, mesmas credenciais dos reseeds
      manuais já registrados nas notas acima — hash BCrypt custo 12 de
      `TrocarEssaSenha123` pré-computado, `must_change_password=TRUE`).
      **"Forçando troca" tem dentes, não é só um flag decorativo**: novo
      `MustChangePasswordFilter` (`config/security`, `@Component`, mesmo padrão de
      registro global do `RateLimitFilter`) bloqueia com 403 qualquer chamada a
      `/admin/api/**` de um platform admin com `mustChangePassword=true`, exceto a própria
      rota nova `POST /admin/api/v1/platform-admins/me/password` (self-service,
      `ManagePlatformAdminUseCase.changeOwnPassword`, exige a senha atual). **Decisão de
      design**: o filtro consulta o banco a cada requisição em vez de confiar num claim
      `must_change_password` do JWT (que também foi adicionado ao token, seção 7.2, para
      uso futuro de UI) — um claim fixado na emissão do token ficaria obsoleto assim que a
      senha fosse trocada, prendendo o admin até o token expirar (até 60 min, ver nota de
      TTL acima) mesmo depois de já ter trocado a senha corretamente.
      `PlatformAdmin.changePassword()` (domínio) limpa `mustChangePassword` automaticamente
      — nenhum outro caminho de código precisa lembrar de fazer isso.
      **Achado durante a implementação**: a migration de seed insere um platform admin
      ativo em **toda** base nova, inclusive a do Testcontainers usada pelos testes de
      integração — quebrava duas asserções de contagem absoluta
      (`PlatformAdminRepositoryIntegrationTest.deveContarAdminsAtivos`/
      `desativarNaoDeveContarComoAtivo`) e a premissa de "último admin ativo" de
      `PlatformAdminControllerIntegrationTest.deveRejeitarDesativarUltimoPlatformAdminAtivo`.
      Corrigido tornando as contagens relativas a uma baseline medida no início de cada
      teste, e desativando explicitamente qualquer admin pré-existente (o seedado incluído)
      antes de testar a rejeição do último ativo — não desabilitei nem afrouxei nenhuma
      asserção. Novo `SeedPlatformAdminIntegrationTest` (3 testes) cobre o cenário completo:
      login do admin seedado retornando `mustChangePassword:true`, bloqueio de
      `/admin/api/v1/tenants` até a troca, desbloqueio depois de trocar com sucesso, e
      rejeição da troca com senha atual incorreta. 9 testes novos/alterados no total (domain,
      unit de application, integração). `mvn verify` verde (suíte completa + ArchUnit + gate
      JaCoCo, cobertura domain+application em 92,6%).

Fase 10 completa.

## Fase 11 — Deploy AWS
- [ ] `Dockerfile` multi-stage (build Maven → runtime JRE slim)
- [ ] `docker-compose.prod.yml`: auth-server + postgres (volume EBS) + nginx
- [ ] EC2 t4g.small, EBS separado montado para os dados do Postgres
- [ ] Nginx: TLS (Let's Encrypt), reverse proxy, servindo o Angular
- [ ] Script de `pg_dump` diário → S3 (versioning + lifecycle de 30 dias)
- [ ] Teste de restore — backup não testado não é backup
- [ ] Segredos por env var, fora da imagem e do repositório
- [ ] CloudWatch Agent para logs e métricas
- [ ] GitHub Actions: build → test → imagem → deploy
- [ ] Health checks do Actuator + alarme de indisponibilidade
- [ ] Decidir e implementar controle de acesso do Actuator (`/actuator/**` hoje não bate
      em nenhum `securityMatcher` do `SecurityConfig` — passa sem autenticação nenhuma,
      ver Notas da Fase 8)

---

## Gaps conhecidos — trabalho futuro, não planejado ainda

- **Não existe hoje um admin intermediário, escopado ao próprio tenant** (só
  `PlatformAdmin`, que vê todos os tenants, e `User`, que só autentica). Avaliado em
  2026-08-28 a pedido do usuário. Desenho para quando este item for priorizado:
  - **Não criar um novo agregado.** Adicionar um flag `isTenantAdmin` diretamente em
    `User` (mesma tabela, mesmo tenant) — análogo a como `PlatformAdmin` é ortogonal a
    tenant, mas aqui o admin **é** um usuário do tenant, só que elevado. Evita duplicar
    autenticação/reset de senha/bloqueio por tentativas, que `User` já tem.
  - **Não modelar como `SystemProfile`.** Um `SystemProfile` (`ADMIN`, `OPERADOR`, ...)
    é definido pelo *sistema* (contrato de negócio de terceiro — regra herdada explicitamente
    para os satélites, ver `CLAUDE.md` do `sistema_promissorias`: "autorização é por
    permissão, nunca por perfil" e "o Auth Server entrega só `profiles`"). "Administrar
    o tenant" é uma capacidade do **auth server sobre si mesmo**, não uma permissão de
    negócio de terceiro — misturar os dois violaria o próprio princípio de design do
    projeto (seção 2, linha 18 do plano: o auth server nunca decide "este usuário pode
    executar esta ação", só "quem é e quais perfis tem").
  - **JWT:** quando `user.isTenantAdmin == true`, `JwtTokenCustomizer` acrescenta a
    autoridade `ROLE_TENANT_ADMIN` ao token do usuário (além de `profiles`), convivendo
    com o `tenant_id` que o token de usuário já carrega.
  - **Autorização nos controllers `/admin/api/v1/tenants/{tenantId}/...`:** trocar
    `hasAuthority('ROLE_PLATFORM_ADMIN')` fixo por uma expressão que também aceita
    `ROLE_TENANT_ADMIN` **desde que** `tenantId` do path bata com o `tenant_id` do token
    — único ponto do sistema que passaria a precisar de checagem de tenant no controller
    (hoje inexistente porque só platform admin acessa `/admin/api/**`, seção 8.1/8.2).
    Ainda como parâmetro explícito (SpEL comparando path vs. claim), nunca `ThreadLocal`
    — consistente com a decisão já tomada na seção 8.2 do plano.
  - **Escopo do que o tenant admin pode fazer** (dentro do seu próprio `tenantId`):
    - Permitido: CRUD de `User` do tenant; vínculos `UserSystem`/`UserSystemProfile`,
      mas **só** para `System` já vinculado ao tenant (`SystemTenant` existente) e **só**
      com códigos de `SystemProfile` já cadastrados — nunca criar/editar o catálogo de
      perfis.
    - Proibido: criar `Tenant`; criar/editar `System`; criar `SystemTenant` (vincular
      sistema novo ao tenant); criar/editar `SystemProfile` (catálogo de perfis é
      contrato do sistema, definido por quem integra o sistema, não pelo cliente do
      tenant); gerenciar `PlatformAdmin`.
    - Em aberto, decisão do usuário quando chegar a hora: se um tenant admin pode
      promover outro usuário do mesmo tenant a tenant admin (self-service) ou se isso
      fica só com platform admin (mais seguro contra escalação dentro do próprio
      tenant, mais fricção operacional).
  - Sem migration nova além de uma coluna booleana em `user` (default `false`). Sem
    tabela nova.

## Notas

- **Access token TTL aumentado de 15 min para 60 min** (`SystemRegisteredClientRepository`,
  2026-08-21), a pedido do sistema satélite `sistema_promissorias`. Motivo: clients
  públicos PKCE (`ClientAuthenticationMethod.NONE`) nunca recebem `refresh_token` do
  Spring Authorization Server — é uma restrição deliberada do framework (não emite
  refresh token para `NONE` independente de `AuthorizationGrantType.REFRESH_TOKEN`
  estar registrado; confirmado nas issues oficiais do projeto, ex. spring-authorization-server#296),
  não uma configuração que faltava aqui. Sem refresh token, a única forma de renovar é
  refazer o `authorization_code` flow via redirect completo, o que recarrega a página
  inteira do SPA consumidor a cada expiração — problemático para formulários longos.
  Aumentar o TTL não resolve a causa raiz (só reduz a frequência do redirect); a correção
  completa exigiria um client confidencial com BFF (recomendação oficial do Spring para
  SPA), que é uma mudança de arquitetura maior, fora de escopo por ora. TTL continua
  **hardcoded e global** (mesmo valor para console admin e todo sistema satélite); tornar
  configurável (por env var ou por client) é melhoria futura, não decidida ainda.
- **Rotas de perfil aninhadas sob `/systems/{systemId}/profiles/{id}`, não
  `/profiles/{id}` como a tabela da seção 9 sugere.** `ManageProfileUseCase` exige
  `systemId` **e** `id` em toda operação além de criar/listar (`SystemProfile` só existe
  dentro de um sistema, `UNIQUE (systemId, code)`) — usar a rota flat exigiria uma busca
  extra só para descobrir a qual sistema um `id` de perfil pertence. Mesmo raciocínio já
  aplicado ao `DELETE` de redirect URI do `SystemController`.
- **`SecurityConfig` não tem filter chain "catch-all".** Cada um dos 4 filter chains
  (`/oauth2/**`, `/admin/api/**`, `/api/auth/**`, docs OpenAPI) usa `securityMatcher(...)`
  restrito ao seu prefixo — qualquer caminho que não bata em nenhum deles (hoje:
  `/actuator/**`) passa batido pelo Spring Security inteiro, sem nenhum filtro de
  autenticação/autorização aplicado. `/actuator/health,info,metrics,prometheus` está
  exposto sem autenticação agora. Não é urgente enquanto só a suíte de testes acessa a
  aplicação, mas **precisa ser resolvido antes do deploy real** (Fase 11) — decidir se
  actuator fica atrás de rede interna (sem exigir token) ou ganha seu próprio filter chain
  com auth. Registrar como item explícito do checklist da Fase 11 ao chegar lá.
- **OpenAPI só documenta `/admin/api/v1/**` e exige o mesmo token de platform admin dos
  endpoints que documenta.** `/api/auth/**` é consumido pelo SPA Angular (Fase 9), não por
  integradores externos — não teria valor documentá-lo via Swagger. Igual raciocínio de
  acesso: a documentação descreve toda a superfície administrativa (nomes de campo, rotas,
  DTOs), então fica atrás do mesmo controle de acesso que ela descreve, não pública.
- **Nunca registre um `JsonMapper`/`ObjectMapper` customizado como `@Bean` sem `@Qualifier`
  dedicado.** Descoberto ao escrever os testes do `SystemController` (Fase 8): o
  `oauth2AuthorizationJsonMapper` da Fase 6 era um `@Bean JsonMapper` — como era o único
  `JsonMapper` no contexto, a autoconfiguração do Spring Boot desistiu de criar o seu
  próprio default e todo `HttpMessageConverter` do Spring MVC passou a usar aquele mapper,
  cujo `PolymorphicTypeValidator` só permite os pacotes internos do Authorization Server.
  Resultado: `POST` com qualquer `List<String>` no corpo (ex: `initialRedirectUris`)
  quebrava com `HttpMessageNotReadableException` em QUALQUER controller da aplicação, não
  só nos endpoints OAuth2 — silencioso porque nenhum teste de outro controller existia
  ainda quando a Fase 6 foi commitada. Corrigido convertendo
  `OAuth2AuthorizationJsonMapperConfig` (bean) em `OAuth2AuthorizationJsonMapperFactory`
  (classe utilitária comum, `build()` estático, chamada diretamente em
  `AuthorizationServerConfig` — nunca entra no contexto Spring). Regra geral: um
  `JsonMapper`/`ObjectMapper` com allowlist restrito só deve ser usado onde é
  explicitamente construído e passado, nunca exposto como bean genérico.
- **Consentimento OAuth2 implementado na própria Fase 8** (`System.thirdParty`,
  `JdbcOAuth2AuthorizationConsentService`, `POST /api/auth/consent`), fechando o item
  adiado da Fase 7. Decisões de design:
  - `System.thirdParty` (migration V14) controla `requireAuthorizationConsent` em
    `SystemRegisteredClientRepository` — sistemas próprios (`false`, default) nunca pedem
    consentimento; parceiros externos (`true`) sempre pedem. Exposto só na criação
    (`POST /admin/api/v1/tenants/{tenantId}/systems`), sem endpoint dedicado para alterar
    depois — o plano não pede isso e não há caso de uso óbvio para "promover" um sistema
    existente a terceiro.
  - Todo `RegisteredClient` ganhou o scope fixo `"profile"` — o projeto não usa scopes
    OAuth2 para controlar acesso (isso é papel do claim `profiles` do token, seção 7.2),
    então esse scope só existe para o fluxo de consentimento ter algo concreto para
    exibir/gravar. Sem ele, `GET /oauth2/authorize?scope=...` falharia com
    `invalid_scope` antes mesmo de chegar no consentimento.
  - `SecurityConfig.authorizationServerSecurityFilterChain` aponta
    `authorizationEndpoint.consentPage("/consent")` — mesmo padrão do `/login` (seção 2.2):
    Spring redireciona para essa rota Angular com `client_id`/`scope`/`state` na URL
    quando consentimento é necessário e ainda não foi dado.
  - `POST /api/auth/consent` (`AuthController`) NÃO reenvia a requisição a
    `/oauth2/authorize` internamente (diferente do POST nativo do Spring Authorization
    Server) — só grava o `OAuth2AuthorizationConsent` via
    `OAuth2AuthorizationConsentService.save(...)` e retorna 200. É o Angular que refaz
    `GET /oauth2/authorize` em seguida (mesma navegação de dois passos do login), que
    agora sucede porque o consentimento já existe para aquele
    `(registeredClientId, principalName)`.
  - `JdbcOAuth2AuthorizationConsentService` não precisou do `JsonMapper` customizado que
    `OAuth2Authorization` exige (nota acima) — authorities de um consentimento são só uma
    lista de strings de scope, não um `Authentication` completo.
- **`UserRepository.findById(UserId)` é uma exceção deliberada à regra do TenantId
  obrigatório** (seção 6.5): é busca por chave primária, não por critério de pesquisa,
  então não há risco de vazamento entre tenants. Único consumidor por enquanto:
  `ResetPasswordService.confirmReset`, que só tem o `userId` guardado no token de reset
  (a tabela `password_reset_token` não tem coluna de tenant).
- **Gestão de Platform Admin ganhou use case na própria Fase 8.** A Fase 4 excluiu
  platform admin deliberadamente ("tenant, sistema, perfil, usuário e vínculos"), então
  `ManagePlatformAdminUseCase`/`PlatformAdminManagementService` foram criados só agora,
  ao construir `/admin/api/v1/platform-admins` (seção 9). Primeiro consumidor real de
  `PlatformAdminPolicy` (Fase 2) — a regra "nunca desativar o último admin ativo" só
  passou a ser exercitada com este controller.
- **Domain services exigem `@Bean` explícito.** `TenantConsistencyValidator`,
  `AccessValidator`, `PlatformAdminPolicy` e `ProfileUniquenessPolicy` são POJOs puros
  (regra 5.1.1) e não têm `@Component`. `config/DomainServicesConfig` os registra como
  beans Spring — qualquer novo domain service usado por um serviço de aplicação precisa
  ser adicionado lá, senão o contexto falha ao subir (`NoSuchBeanDefinitionException`),
  erro que só aparece em teste de integração/contexto completo, não em teste unitário
  com Mockito.
- **Testcontainers: container Postgres "singleton" iniciado manualmente.**
  `AbstractPostgresIntegrationTest` inicia o container num bloco estático, sem as
  anotações `@Testcontainers`/`@Container` — essa combinação em campo `static`
  chama `stop()` no `afterAll` da primeira classe de teste que o referencia, mesmo
  sendo compartilhado entre classes, derrubando o container para todas as classes de
  teste seguintes (reproduzido: primeira classe passa, todas as outras falham com
  "Connection refused" após ~30s de timeout cada). Sem essas anotações, o container só
  é finalizado pelo Ryuk ao fim da JVM de teste. Se novos testes de integração
  passarem a falhar em lote com erro de conexão, comece por aqui.
- **Frontend 100% Angular** (decisão D6 do plano, ajustada em 2026-08-08 a pedido explícito
  do usuário): login e consentimento deixaram de ser Thymeleaf e passaram a ser rotas
  públicas do mesmo SPA Angular do console administrativo. A dependência `thymeleaf` foi
  removida do `pom.xml`; a Fase 7 virou "API de autenticação e consentimento" (só backend
  REST/JSON) e a Fase 9 passou a cobrir login + consentimento + console num único projeto
  Angular. Ver seção 2.2 do plano para o fluxo detalhado.
- **Bloqueio por N tentativas de login adiado para a Fase 7.** A Fase 6 cobre a
  infraestrutura de segurança (filter chains, rate limiting por IP), mas o contador de
  tentativas falhas é por usuário e só faz sentido acoplado ao endpoint `POST
  /api/auth/login` (ainda não existe — é o primeiro item da Fase 7). Implementar o
  bloqueio agora exigiria um campo/estado no `User` sem nenhum consumidor até lá.
- **Filter chain do `/oauth2/**` precisa ser declarado explicitamente em `SecurityConfig`,
  não fica a cargo da autoconfiguração do Spring Boot.** A autoconfiguração
  (`OAuth2AuthorizationServerWebSecurityConfiguration`) só ativa seu próprio
  `SecurityFilterChain` via `@ConditionalOnMissingBean(SecurityFilterChain.class)` — como
  o projeto já declara os filter chains de `/admin/api/**` e `/api/auth/**`, ela nunca
  dispara e `/oauth2/authorize` cai como 404. O bean
  `authorizationServerSecurityFilterChain` em `SecurityConfig` replica manualmente o que a
  autoconfiguração faria (via `OAuth2AuthorizationServerConfigurer` + `.oidc(...)`),
  com `@Order(0)` (mais precedente que os outros dois). Descoberto via bytecode da
  autoconfiguração (`javap`), não pela documentação.
- **MockMvc: `.param()` não popula `request.getQueryString()`.** Os endpoints do
  Authorization Server (`OAuth2AuthorizationCodeRequestAuthenticationConverter`) leem
  parâmetros de uma requisição GET filtrando por `request.getQueryString()`, não só por
  `request.getParameterMap()` — com `.param()` (que só popula o parameter map), todo
  parâmetro é descartado e a requisição falha com `invalid_request: response_type` mesmo
  com os parâmetros visivelmente presentes no dump do `MockHttpServletRequest`. Testes GET
  para `/oauth2/authorize` (e qualquer novo teste MockMvc contra esse endpoint) devem usar
  `.queryParam(...)`, não `.param(...)`.
- **`JdbcOAuth2AuthorizationService` não sabe (des)serializar `Authentication`
  customizado.** O resource owner autenticado (`ClientAwareAuthenticationToken` com
  principal `AuthenticatedUser`) é persistido como parte de `OAuth2Authorization.attributes`
  ao emitir o `code`; sem um `JsonMapper` (Jackson 3) ciente dessas classes, a troca do
  código por token falha ao reler a authorization (`PolymorphicTypeValidator` rejeita o
  tipo). Resolvido com mixins em `config/security/jackson/` (fora do `domain`, para não
  violar a regra 5.1) + um `JsonMapper` dedicado injetado em `JdbcOAuth2AuthorizationService`
  via `setAuthorizationRowMapper`/`setAuthorizationParametersMapper` em
  `AuthorizationServerConfig`. Qualquer novo campo/VO usado dentro de `AuthenticatedUser`
  precisa de um mixin correspondente, senão o mesmo erro volta.
- **Bloqueio por tentativas fica no `User` (domínio), não num serviço à parte.**
  `failedLoginAttempts`/`lockedUntil` são estado do próprio agregado — `registerFailedLoginAttempt()`
  incrementa e bloqueia por `User.LOCKOUT_DURATION` ao atingir `User.MAX_FAILED_LOGIN_ATTEMPTS`;
  `registerSuccessfulLogin()` zera os dois. `AccessValidator.validateLoginAccess` passou a
  checar `user.isLocked()` (mesma mensagem genérica de sempre). `AuthenticationService.authenticate`
  deixou de ser `@Transactional(readOnly = true)` porque agora persiste o `User` mutado em
  ambos os caminhos (falha e sucesso). Migration `V12__add_login_lockout_to_user.sql`.
- **`RateLimitFilter` tem limites configuráveis via `authserver.rate-limit.*` — necessário
  para os testes de integração não se autoderrubarem.** O bucket é um bean singleton
  compartilhado por todo o contexto Spring; como os testes de integração reutilizam o
  mesmo contexto entre classes, chamadas a `/api/auth/login` em testes diferentes somam
  no mesmo bucket por IP. `application-dev.yml` (perfil sob o qual a suíte roda) sobe os
  limites para praticamente ilimitado; produção usa os defaults de `RateLimitFilter`
  (10/30/60, seção 7.4). Qualquer novo teste de integração que bata repetidamente em
  `/api/auth/login`, `/oauth2/token` ou `/admin/api/**` deve continuar contando com esse
  override — não reduzir os limites do perfil `dev`.
- **Como o console administrativo Angular (não vinculado a nenhum tenant) vira um OAuth2
  client ainda está em aberto.** `SystemRegisteredClientRepository` só resolve `System`
  (sempre vinculado a exatamente um tenant, seção 4.4). O client do console precisa de um
  registro que não dependa de tenant — provavelmente um client estático configurado via
  `application.yml`/env vars, combinado ao `SystemRegisteredClientRepository` por um
  `RegisteredClientRepository` delegante. Não decidido nem implementado; retomar na Fase 8
  ou 9 quando o console precisar de fato se autenticar.
- Item "Testes ArchUnit das regras da seção 5.1" (Fase 10) já tem um teste inicial em
  `ArchitectureTest` desde a Fase 0. Usa `allowEmptyShould(true)` para não quebrar a suíte
  enquanto os pacotes `application`/`adapter` ainda não têm código — cada regra passa a
  valer de fato (e falharia de verdade) assim que houver uma violação real. O item da
  Fase 10 fecha quando a cobertura de regras estiver completa (incluindo as de `adapter`
  não vazar entidade JPA e DTO não entrar em `application`).
- **`LazyInitializationException` em `SystemEntity.redirectUris` fora de transação —
  invisível para a suíte de testes, só apareceu ao rodar contra um Postgres real (Fase 9,
  smoke test manual do login Angular).** `SystemRegisteredClientRepository` (chamado direto
  pelo filter chain do Spring Security, sem `@Transactional` de serviço de aplicação por
  cima) lia `System.redirectUris` — `@OneToMany` lazy por padrão — fora de qualquer sessão
  Hibernate aberta. `AbstractRepositoryIntegrationTest` é `@Transactional`, então todo teste
  de integração mantém a sessão aberta e nunca reproduz o erro. Corrigido com
  `@EntityGraph(attributePaths = "redirectUris")` em `SystemJpaRepository.findById`/
  `findByClientId`, e um novo método `findAllWithRedirectUris` (idem) usado por
  `SystemRepositoryImpl.findAll`. Regressão coberta em
  `SystemRepositoryIntegrationTest.deveCarregarRedirectUrisForaDeUmaTransacao`, que usa
  `@Transactional(propagation = Propagation.NOT_SUPPORTED)` para replicar de fato a ausência
  de transação (com limpeza manual do registro no `finally`, já que não há rollback
  automático). **Lição geral:** qualquer repositório JPA chamado por infraestrutura do
  Spring Security (fora do ciclo normal de use case → `@Transactional`) precisa de
  `@EntityGraph` em toda coleção lazy que o mapper de domínio acessa — só um teste com
  `Propagation.NOT_SUPPORTED` pega esse tipo de bug antes de produção.
- **`ConsentComponent`/`ForgotPasswordComponent`/`ResetPasswordComponent` (Fase 9) só têm
  verificação automatizada (testes unitários Angular + `ng build` de produção), não smoke
  test manual em navegador como o `/login`.** O fluxo de consentimento exige um `System`
  com `thirdParty = true` para o backend redirecionar para `/consent` de verdade (nenhum
  system de teste manual criado até agora tem essa flag), e `/esqueci-senha`/
  `/reset-password` exigem receber um e-mail real do Mailhog e extrair o token — cobertura
  ponta a ponta real já existe no backend (`OAuth2ConsentFlowIntegrationTest`,
  `ResetPasswordServiceIntegrationTest` e afins), então a UI ficou coberta só até onde os
  testes de componente alcançam. Retomar com smoke test manual quando o console
  administrativo (CRUD de sistemas) permitir marcar `thirdParty=true` pela própria UI, ou
  antes do deploy real (Fase 11).
- **Como o console administrativo Angular vira um OAuth2 client foi resolvido nesta
  rodada** (a nota antiga "não decidido nem implementado" acima está desatualizada).
  Decisões de design:
  - `RegisteredClientRepositoryConfig` (novo, `config/security`) monta um `RegisteredClient`
    estático (não vem da tabela `system` — o console não pertence a nenhum tenant, D3/D9
    exigem 1 sistema : 1 tenant) via `InMemoryRegisteredClientRepository`, combinado com
    `SystemRegisteredClientRepository` por meio de um novo
    `CompositeRegisteredClientRepository` (`adapter/out/security/oauth2`, primeiro delegate
    que resolver o client vence). `SystemRegisteredClientRepository` deixou de ser
    `@Component` — dois beans `RegisteredClientRepository` no contexto tornariam ambíguo
    todo ponto de injeção da aplicação; agora só o `@Bean` da config o constrói.
  - `authserver.console-client.client-id`/`redirect-uris` (novo em `application.yml`,
    default `console` / `${issuer}/console/callback`, override em `application-dev.yml` para
    `http://localhost:4200/console/callback`).
  - **Login de platform admin passa pelo mesmo `POST /api/auth/login`** dos usuários de
    tenant. Antes desta rodada isso não funcionava de verdade:
    `PlatformAdminAuthenticationProvider.supports()` só aceitava
    `UsernamePasswordAuthenticationToken`, mas `AuthController.login()` sempre constrói um
    `ClientAwareAuthenticationToken` — o `ProviderManager` nunca chegava a tentar esse
    provider para o request real do controller (só o teste unitário, construído à mão com
    `UsernamePasswordAuthenticationToken`, mascarava isso). Corrigido fazendo
    `PlatformAdminAuthenticationProvider` suportar os dois tipos de token — quando
    `UserAuthenticationProvider` falha (client_id "console" não resolve nenhum `System`),
    o `ProviderManager` cai para este provider, que ignora o `clientId` e autentica só por
    usuário/senha (mesmo comportamento de sempre, seção 2.1).
  - Novo record de transporte `AuthenticatedPlatformAdmin` (`application/model`, análogo a
    `AuthenticatedUser`) substitui o agregado `PlatformAdmin` como principal do
    `Authentication` de sessão — evita carregar o hash de senha do agregado até
    `OAuth2Authorization.attributes` (`JdbcOAuth2AuthorizationService`) e mantém o padrão já
    usado para usuário de tenant. `JwtTokenCustomizer.customizeForPlatformAdmin` e
    `LoginResponse.from(...)` (nova sobrecarga) passaram a usá-lo; novos mixins Jackson
    (`AuthenticatedPlatformAdminMixin`, `PlatformAdminIdMixin`) em
    `OAuth2AuthorizationJsonMapperFactory`.
  - **Dois bugs de configuração só apareceram no smoke test manual em navegador** (nenhum
    teste MockMvc os pega, porque nenhum executa `angular-oauth2-oidc` de verdade):
    1. CORS não cobria `/.well-known/**` — `OAuthService.loadDiscoveryDocument()` do console
       é um `fetch` cross-origin real em dev (`ng serve` em `:4200`, backend em `:8080`);
       sem CORS nesse path, o browser bloqueia a resposta antes do JS conseguir lê-la.
       `CorsConfig` ganhou `/.well-known/**` na lista de patterns.
    2. `authserver.issuer`/`spring.security.oauth2.authorizationserver.issuer` nunca tinham
       override em `application-dev.yml` — o documento de descoberta OIDC sempre declarava
       o issuer de produção (`https://auth.seudominio.com`) mesmo rodando em
       `localhost:8080`. `angular-oauth2-oidc` valida `doc.issuer === this.issuer` e rejeita
       silenciosamente (só loga erro) quando não bate — o guard do console falhava sem
       nenhuma exceção visível. `application-dev.yml` agora sobrescreve os dois para
       `http://localhost:8080` por padrão.
  - **Gap estrutural de dev, sem bug em produção**: `SpaLoginAuthenticationEntryPoint` e
    `authorizationEndpoint.consentPage(...)` sempre usaram caminho relativo (`/login`,
    `/consent`) — correto em produção, onde nginx serve Angular e backend na mesma origem
    (seção 11). Em dev, uma chamada de verdade a `GET /oauth2/authorize` sem sessão (como o
    `initCodeFlow()` do console faz) redireciona para `http://localhost:8080/login`, que não
    existe (Angular só é servido em `:4200` via `ng serve`) — 404 Whitelabel. Isso sempre
    afetou o fluxo de login de usuário de tenant também, só nunca foi exercitado de verdade
    (os testes manuais anteriores sempre navegavam direto para `:4200/login?...` a mão,
    pulando o redirect real). Resolvido tornando os dois configuráveis
    (`authserver.frontend.login-url`/`consent-url`, novo, default `/login`/`/consent` em
    todo profile — **não** override em `application-dev.yml`, para não mudar o
    comportamento validado por `SpaLoginAuthenticationEntryPointIntegrationTest`). Para
    testar manualmente com `ng serve` + `mvn spring-boot:run` em portas separadas, exportar
    `LOGIN_URL=http://localhost:4200/login CONSENT_URL=http://localhost:4200/consent` só
    nessa sessão manual.
  - Frontend: `ConsoleAuthService` (`core/services`) encapsula `OAuthService`
    (`angular-oauth2-oidc`, `provideOAuthClient()` em `app.config.ts`); `consoleAuthGuard`
    (`core/guards`) dispara `initCodeFlow()` quando não há token válido;
    `ConsoleCallbackComponent` (`/console/callback`) troca código por token
    (`tryLoginCodeFlow`); `ConsoleDashboardComponent` (`/console`, atrás do guard) só decodifica
    e exibe os claims do access token (`core/util/jwt.ts`) — placeholder até o CRUD
    administrativo (próximo item do checklist).
  - Verificado ponta a ponta manualmente: login como platform admin → `/oauth2/authorize` →
    `/console/callback` (troca de código) → dashboard exibindo "Administrador (root_admin)".
- **Build de produção + nginx (último item da Fase 9) verificado com um nginx real
  (Docker, `nginx:alpine`) servindo `dist/frontend/browser` e fazendo reverse proxy para o
  backend** (`/oauth2/**`, `/.well-known/**`, `/api/auth/**`, `/admin/api/**`,
  `/v3/api-docs|swagger-ui` → `auth-server:8080`; tudo mais cai em `try_files ... /index.html`
  para o Angular Router funcionar em deep link/refresh). Config nova: `frontend/nginx.conf`,
  reaproveitável pela imagem Docker da Fase 11 (`auth-server` é o nome de serviço Compose
  esperado). Dois bugs reais só apareceram rodando o build de produção de verdade atrás do
  nginx — nenhum teste automatizado ou smoke test anterior (sempre contra `ng serve`) os
  pega:
  1. **`nginx.conf` inicial usava `proxy_set_header Host $host`, que remove a porta** — o
     `Location` do redirect de `SpaLoginAuthenticationEntryPoint` virava
     `http://localhost/login?...` (sem `:8090`), quebrando a navegação same-origin que é a
     razão de existir do nginx aqui. Trocado para `$http_host` (preserva a porta) nos 5
     blocos `proxy_pass`.
  2. **`ConsoleAuthService` configurava `requireHttps: environment.production`** — no build
     de produção (`environment.production = true`), isso rejeita QUALQUER issuer HTTP,
     inclusive `http://localhost` usado para testar o build de produção localmente sem
     TLS. O guard falhava silenciosamente (a exceção de `loadDiscoveryDocument()` era
     engolida dentro de uma promise não observada — só apareceu marcando `document.title`
     passo a passo, já que `console.log`/`console.error` de builds de produção não foram
     capturados pela ferramenta de leitura de console usada para depurar). Trocado para
     `requireHttps: 'remoteOnly'` — exige HTTPS para qualquer domínio real (produção sempre
     é TLS via nginx/Let's Encrypt, seção 11) mas permite `http://localhost` explicitamente,
     sem enfraquecer a checagem em produção de verdade.
  - **Lição de depuração**: ao investigar um guard/rota que parece "não fazer nada" num
    build de produção servido por nginx/Docker, não confie apenas em
    `read_console_messages` do Chrome — logs de builds otimizados podem não ser capturados
    pela ferramenta de leitura de console a tempo. Marcar `document.title` em pontos-chave
    do fluxo é um canal mais confiável para localizar onde a execução realmente para.
  - Smoke test manual: `docker run nginx:alpine` com `-v dist/frontend/browser:/usr/share/nginx/html`
    e `-v frontend/nginx.conf:/etc/nginx/conf.d/default.conf`, `--add-host=auth-server:host-gateway`
    apontando para o backend rodando via `mvn spring-boot:run` no host — login completo como
    platform admin, PKCE do console, e `/admin/api/v1/tenants` (bearer token) todos
    funcionando através do nginx em `http://localhost:8090`. Containers de teste removidos
    ao final (`authserver-nginx-test*`), não fazem parte do projeto committado.
- **Três bugs reais encontrados só ao executar o roteiro manual da seção 14 do plano**
  (`docker-compose` → `mvn verify` → login no console → criar tenant/sistema/perfis →
  vincular → PKCE → validar claims) — nenhum apareceu antes porque nenhum teste automatizado
  exercitava o caminho completo "platform admin de verdade criando um recurso auditado pelo
  console de verdade":
  1. **`created_by`/`sub` do JWT de platform admin estourava `VARCHAR(50)`** —
     `AuthenticatedPlatformAdmin` não implementava `AuthenticatedPrincipal`, então
     `Authentication.getName()` caía no `toString()` do record inteiro. Corrigido
     implementando `AuthenticatedPrincipal` (`getName()` retorna só o ID).
  2. **Efeito colateral da correção acima**: `getName()` colidiu com o record accessor
     `name()` na introspecção do Jackson usado para persistir
     `OAuth2Authorization.attributes` — o claim `name` do JWT saía com o ID em vez do nome
     de exibição. Corrigido renomeando o campo do record para `displayName`.
  3. **Todo ID (TSID) trafegado como número JSON perdia precisão no Angular** —
     `Number.MAX_SAFE_INTEGER` do JavaScript é menor que TSIDs de 64 bits; um ID virava
     outro ID ao dar round-trip pelo `JSON.parse` do browser, quebrando qualquer operação
     que dependesse do ID exato (ex: criar um sistema sob um tenant recém-criado falhava
     com "Tenant não encontrado"). Corrigido serializando todo campo de ID como `String`
     em todos os DTOs administrativos (backend) e nos models/serviços/componentes
     correspondentes (frontend).
  - Ver commits `fix(security)`, `fix(admin-api)` e `fix(frontend)` logo antes desta nota
    para os detalhes de cada um. **Lição geral**: testes automatizados que constroem um
    JWT "de brinquedo" à mão (`.subject("1")`, por exemplo) ou que nunca serializam/
    desserializam de verdade pela borda HTTP/JSON não pegam essa classe de bug — só um
    teste manual ponta a ponta contra o sistema real pegou os três.
- **Roteiro manual da seção 14 concluído de ponta a ponta** (passos 2 a 11; passo 12,
  `pg_dump`/restore, é explicitamente Fase 11 e fica para lá). Um quarto bug real apareceu
  já no passo 6 (criar usuário):
  4. **`POST /admin/api/v1/tenants/{id}/users` falhava com 500 sempre que
     `EmailSenderPort.sendWelcomeEmail` era chamado em dev/e2e contra o Mailhog.**
     `application.yml` (produção) fixa `spring.mail.properties.mail.smtp.auth: true` e
     `starttls.enable: true` — corretos para SMTP real (Gmail/SendGrid/etc.), mas
     `application-dev.yml` só sobrescrevia `host`/`port`/`username`/`password`, não essas
     duas flags. Sem usuário/senha configurados (Mailhog não exige nenhum dos dois),
     `JavaMailSenderImpl` ainda tentava autenticar por causa de `auth: true`, e
     `jakarta.mail.AuthenticationFailedException: failed to connect, no password
     specified?` subia sem tratamento — como o envio do e-mail acontece dentro da mesma
     `@Transactional` de `UserManagementService.createUser`, a criação inteira do usuário
     era revertida. Corrigido sobrescrevendo `auth: false`/`starttls.enable: false` em
     `application-dev.yml`. **Lição**: como `EmailSenderPort` é chamado dentro da mesma
     transação do caso de uso, qualquer falha de infraestrutura de e-mail (config errada,
     servidor fora do ar) derruba a operação de negócio inteira — nenhum teste automatizado
     pega isso porque os testes de integração usam um `EmailSenderPort` fake/mock, nunca o
     `SmtpEmailSender` real contra um servidor de verdade.
  - Passos 5, 7, 8, 9, 10 e 11 confirmados exatamente como descrito no plano: perfil
    `ADMIN` repetido em `CRM_GLOBEX` (código único por sistema, não global); vínculo
    usuário↔sistema cross-tenant rejeitado com 400 e mensagem clara; PKCE completo como
    usuário de tenant (não platform admin) retornando `tenant_id`/`client_id`/
    `profiles: ["ADMIN"]` corretos no JWT; assinatura validada contra `/oauth2/jwks` via
    Web Crypto no próprio browser; tenant desativado → login rejeitado com 401 genérico
    ("Invalid credentials", sem vazar a causa).
  - **Bug de UX encontrado mas não corrigido nesta rodada** (fora do escopo do roteiro,
    registrado para retomar): o dashboard do console (`/console`) continua exibindo
    "Logado como Administrador" mesmo com o access token JWT expirado — só falha de fato
    na primeira chamada de API subsequente. `ConsoleAuthService`/`consoleAuthGuard` não
    verificam `expires_at` ao exibir a página, só ao decidir se dispara `initCodeFlow()`.
- **Frontend extraído para repositório próprio em 2026-08-10** (`java_projects/auth_frontend_v2`,
  sibling deste repo, `.git` independente e primeiro commit sem histórico anterior). Deixa
  de ser um monorepo backend+frontend. Motivo: nenhum registrado além de separar o ciclo de
  vida dos dois projetos — decisão do usuário, não decorre de um problema encontrado.
  Consequências que ainda precisam de decisão:
  - **Fase 11 (deploy) previa um único host servindo nginx + estáticos do Angular + auth-server**
    (seção 11 do plano). Com dois repos, o pipeline de deploy (`GitHub Actions`, item da
    Fase 11) precisa buscar/buildar os dois — via submodule, checkout duplo na Actions, ou
    publicando o `dist/frontend/browser` de `auth_frontend_v2` como artefato consumido pelo
    build do backend. Não decidido; retomar ao iniciar a Fase 11.
  - `docs/03-subir-ambiente-local.md` (seção 6) e `README.md` já atualizados para o novo
    layout (`cd ../auth_frontend_v2` em vez de `cd frontend`). O restante das menções a
    "frontend Angular" no plano e neste arquivo é conceitual (não referencia caminho de
    arquivo) e continua válido sem alteração.
- **Reunificado em `sistemas/auth_v2` em 2026-08-29**, seguindo o padrão de monorepo já usado
  em `sistema_promissorias` (pastas `api/` e `web/`, docs comuns na raiz). Histórico deste
  repositório (`auth_api_v2`) preservado via `git subtree add --prefix=api`; o de
  `auth_frontend_v2` preservado do mesmo jeito em `web/`. `docs/03-subir-ambiente-local.md`
  e `README.md` atualizados para `cd ../web` em vez de `cd ../auth_frontend_v2`. Este arquivo
  (`PROGRESS.md`) e o de `web/` continuam separados — cada um documenta seu próprio lado.
- **Revisão de arquitetura — auditoria geral, 2026-08-29** (agente `revisor-arquitetura`,
  escopo: todo `api/`, não um diff específico). `mvn verify` verde (498 testes, 0 falhas),
  `mvn test -Dtest=ArchitectureTest` verde (6/6). Achados:
  - 🔴 BLOQUEANTE — `GlobalExceptionHandler` (`adapter/in/web/common`) mapeia
    `DomainException` para `HttpStatus.BAD_REQUEST` (400); a seção 6.6 do plano exige 422
    para `DomainException` e 400 só para erro de validação. Divergência sistemática, não
    documentada como decisão consciente: replicada em todos os controllers administrativos
    e "fixada" pelos próprios testes de integração, que esperam `isBadRequest()` onde
    deveria ser 422 (`TenantControllerIntegrationTest`, `SystemControllerIntegrationTest`).
    Item de correção adicionado à Fase 8 acima.
  - 🟡 `mvn jacoco:report` falha — `No plugin found for prefix 'jacoco'`, plugin nunca foi
    adicionado ao `pom.xml`. Consistente com a Fase 10 já estar desmarcada; sem ele não há
    como verificar objetivamente o critério de cobertura ≥ 80%.
  - 🟡 `ClientSecret` (`domain/model/system`) usa `BCryptPasswordEncoder` no domínio, mesma
    exceção que a seção 5.1 do plano nomeia textualmente só para o VO `Password`. Não quebra
    `ArchitectureTest` (que tolera qualquer classe de `org.springframework.security.crypto..`),
    mas é uma extensão não escrita da regra — formalizar no plano que a exceção cobre todo
    VO de segredo/senha, não só `Password`, antes que vire precedente informal.
  - 🟡 `ArchitectureTest` ainda não verifica via ArchUnit que nenhuma entidade JPA cruza a
    fronteira de `adapter.out.persistence` nem que nenhum DTO web entra em `application`
    (item já rastreado na Fase 10). Verificado manualmente nesta auditoria via grep — sem
    violação real hoje —, mas sem o teste automatizado uma violação futura só seria pega em
    code review manual.
- **Frontend modernizado (Angular 19→21), 2026-08-29 a 2026-08-31** — mesmo escopo
  funcional da Fase 9 (login, consentimento, console administrativo completo), stack
  inteira reescrita: zoneless (sem `zone.js`), standalone components, Vitest no lugar de
  Karma/Jasmine, PrimeNG 21 com preset próprio (paleta índigo, guia de estilo dedicado em
  `web/docs/GUIA-DE-ESTILO.md`), dark mode com persistência. Histórico fase a fase,
  decisões de arquitetura e todos os achados em `web/PROGRESS.md` — vale destacar os que
  são deste lado do repositório (backend), encontrados a partir do trabalho no frontend:
  - **Bug real, corrigido nesta janela**: `POST /api/auth/logout` não existia — nenhum
    `SecurityFilterChain` tinha `.logout(...)` configurado, então limpar só os tokens OAuth
    no `localStorage` do frontend nunca invalidava a `HttpSession` (`JSESSIONID`); o próximo
    `GET /oauth2/authorize` reautenticava silenciosamente via
    `HttpSessionSecurityContextRepository`, sem pedir login de novo. Endpoint novo em
    `AuthController` chama `session.invalidate()`; teste de integração dedicado
    (`AuthControllerIntegrationTest.deveInvalidarSessaoAoDeslogarImpedindoReautenticacaoSilenciosaNoAuthorize`).
  - **Bug real, não corrigido (fora do escopo da skill de frontend)**: `save()` em um
    `System` já existente falha com 500 genérico — reproduzido em `updateSystem`,
    `addRedirectUri`, `rotateSecret` e `updateSystemStatus`, todos via chamada direta
    (não é bug do frontend). Suspeita: `SystemEntity.redirectUris` mapeado como
    `@OneToMany(cascade = ALL, orphanRemoval = true)`, padrão clássico de erro do Hibernate
    quando o mapper substitui a coleção inteira em vez de mutar in-place. `Tenant`/`Profile`
    não têm esse problema — específico de `System`. Vale investigar/corrigir como item de
    backlog.
  - **Bug real, não corrigido**: `authserver.frontend.login-url` não tem override no
    profile `dev` — cai no default de `application.yml`, relativo à própria API (`:8080`),
    não ao frontend (`:4200`); `SpaLoginAuthenticationEntryPoint` redireciona para uma rota
    inexistente na API. Contorno usado durante o desenvolvimento:
    `LOGIN_URL=http://localhost:4200/login` como env var — vale um default correto em
    `application-dev.yml`.
  - **Risco de produto identificado, não é bug**: desativar o único (ou último) platform
    admin ativo trava o acesso a todo o console — a admin-api não expõe nenhuma checagem
    de "não desativar o último admin" (`PlatformAdminPolicy` cobre isso? conferir — a UI só
    nomeia a consequência na confirmação, não impede o caso extremo).
  - 🟡 Enums persistidos (`status`) são mapeados como `String` puro nas entidades JPA, com
    conversão manual via `valueOf(...)`/`.name()` em `AuthMapper`, em vez de
    `@Enumerated(EnumType.STRING)` como a seção 6.4 descreve literalmente. Funcionalmente
    equivalente (nunca grava ordinal) — divergência de estilo, não de risco.
  - Veredito da revisão: REPROVADO só pelo item bloqueante acima; o restante do código está
    arquiteturalmente saudável.
- **Item bloqueante da revisão de 2026-08-29 corrigido**: `GlobalExceptionHandler.handleDomainException`
  passou a responder `422 UNPROCESSABLE_ENTITY`, não mais 400. Os 400 que continuavam sendo apenas
  `MethodArgumentNotValidException` (Bean Validation, ex: `@NotBlank`/`@NotEmpty`) ficaram como
  estavam; só os `andExpect(status().isBadRequest())` cujo caminho de teste efetivamente lançava
  `DomainException` (código/username/clientId duplicado, última redirect URI, vínculo duplicado ou
  cross-tenant, desativar o último platform admin) foram trocados para
  `status().isUnprocessableEntity()`, em `TenantControllerIntegrationTest`,
  `UserControllerIntegrationTest`, `SystemControllerIntegrationTest`,
  `SystemProfileControllerIntegrationTest`, `PlatformAdminControllerIntegrationTest` e
  `BindingControllerIntegrationTest`. `AuthControllerIntegrationTest` não foi tocado — `adapter/in/web/auth`
  mantém seus próprios `@ExceptionHandler` locais, fora do escopo do `GlobalExceptionHandler`
  (`basePackages = "...adapter.in.web.admin"`). `mvn verify` verde na suíte inteira depois da mudança.
