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
- [ ] Decidir e implementar controle de acesso do Actuator (`/actuator/**` hoje não bate
      em nenhum `securityMatcher` do `SecurityConfig` — passa sem autenticação nenhuma,
      ver Notas da Fase 8)

---

## Notas

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
