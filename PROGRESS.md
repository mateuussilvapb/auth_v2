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
- [ ] Flag de "cliente de terceiro" em `System` (`ManageSystemUseCase`) — pré-requisito do consentimento adiado da Fase 7
- [ ] `POST /api/auth/consent` + `JdbcOAuth2AuthorizationConsentService` (adiado da Fase 7 — ver Notas)
- Controllers da seção 9 (`/admin/api/v1`, DTOs com Bean Validation, testes de integração — item a item):
  - [x] Tenants (`POST` · `GET` · `GET /{id}` · `PUT /{id}` · `PATCH /{id}/status`)
  - [x] Sistemas (`POST` · `GET` · `PUT /{id}` · `PATCH /{id}/status` · redirect-uris · rotate-secret)
  - [ ] Perfis
  - [ ] Usuários
  - [ ] Vínculos
  - [ ] Platform admins
- [ ] OpenAPI/Swagger
- [ ] CORS para o SPA Angular

## Fase 9 — Frontend Angular (login, consentimento e console administrativo)
- [ ] Projeto Angular único: rotas públicas (`/login`, `/consent`, `/esqueci-senha`) consumindo a API da Fase 7, e rotas protegidas do console consumindo a API da Fase 8
- [ ] Cliente OAuth2 PKCE (`angular-oauth2-oidc`) para o console administrativo
- [ ] Tela de login com branding por tenant e mensagens de erro genéricas
- [ ] CRUD de tenants, sistemas, perfis, usuários
- [ ] Tela de vínculos (usuário → sistemas → perfis)
- [ ] Guard de rota exigindo platform admin nas rotas do console
- [ ] Build de produção servido por nginx (mesmo domínio do auth server)

## Fase 10 — Qualidade
- [ ] Testes ArchUnit das regras da seção 5.1
- [ ] Cobertura ≥ 80% no domínio e na aplicação (JaCoCo)
- [ ] Teste end-to-end: criar tenant → sistema → perfis → usuário → vincular → login → validar claims
- [ ] Seed inicial: primeiro platform admin via migration com senha temporária forçando troca

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

---

## Notas

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
- **`POST /api/auth/consent` adiado para a Fase 8.** O plano menciona o endpoint só de
  passagem ("quando existir client de terceiro", seção 2.2) e não define nenhum critério
  para um `System` ser "de terceiro" — hoje `SystemRegisteredClientRepository` grava
  `requireAuthorizationConsent(false)` para todo client, sem exceção, então o fluxo de
  consentimento do Spring Authorization Server nunca dispara (não haveria como testar o
  endpoint de ponta a ponta). Decisão confirmada com o usuário: implementar esse item
  junto da Fase 8, quando `ManageSystemUseCase` ganhar o flag de "cliente de terceiro" que
  o consentimento depende — só faz sentido construir o endpoint depois que existir uma
  forma real de acioná-lo. A tabela `oauth2_authorization_consent` já existe desde a
  migration V11 (Fase 3), só falta o `JdbcOAuth2AuthorizationConsentService` (hoje o
  Authorization Server usa o `InMemoryOAuth2AuthorizationConsentService` default do Boot,
  irrelevante enquanto consentimento nunca é exigido).
- **`UserRepository.findById(UserId)` é uma exceção deliberada à regra do TenantId
  obrigatório** (seção 6.5): é busca por chave primária, não por critério de pesquisa,
  então não há risco de vazamento entre tenants. Único consumidor por enquanto:
  `ResetPasswordService.confirmReset`, que só tem o `userId` guardado no token de reset
  (a tabela `password_reset_token` não tem coluna de tenant).
- **Gestão de Platform Admin ainda não tem use case.** A Fase 4 lista explicitamente
  "tenant, sistema, perfil, usuário e vínculos" — sem platform admin — então
  `ManagePlatformAdminUseCase`/`PlatformAdminManagementService` não foram criados agora
  (o `PlatformAdminPolicy`, da Fase 2, segue sem consumidor). A API administrativa
  (Fase 8) precisa de `POST/GET/PATCH /platform-admins` (seção 9), então esse use case
  precisa existir antes dela — criar quando alguma fase intermediária pedir, ou como
  parte da própria Fase 8 se nenhuma outra reivindicar antes.
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
