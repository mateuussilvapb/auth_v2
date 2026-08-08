# Progresso — Auth Server V2

Espelha os checklists da seção 10 do plano (`# Plano de Implementação — Auth Server v2.md`).
Cada fase deve estar verde nos testes antes da seguinte. Atualizado a cada item fechado.

## Fase 0 — Fundação
- [x] Projeto Spring Boot 4.x, Java 25, Maven (copiar `pom.xml` de referência)
- [x] Remover as dependências `jjwt-*` — resíduo inútil no projeto atual, o Spring Authorization Server já assina o token
- [x] Dependências: web, data-jpa, security, oauth2-authorization-server, validation, flyway, postgresql, mail, actuator, lombok, thymeleaf, hypersistence-tsid, bucket4j, springdoc-openapi
- [x] Testes: testcontainers, archunit, spring-security-test
- [x] `.gitignore` cobrindo `*.pem`, `*.key`, `.env`, `application-local.yml`
- [x] `docker-compose.yml` de desenvolvimento (Postgres + MailHog)
- [x] `README.md` com objetivo, modelo de dados e como subir

## Fase 1 — Domínio: núcleo
- [ ] `shared/`: `DomainId`, `IdGenerator`
- [ ] `exception/DomainException`
- [ ] VOs: `TenantCode`, `Username`, `Email`, `Password`, `ClientId`, `ProfileCode`, `RedirectUri`
- [ ] Enums de status (todos)
- [ ] Entidades: `Tenant`, `PlatformAdmin`, `System`, `SystemProfile`, `User`
- [ ] Testes unitários de cada VO e entidade (feliz + todas as violações de invariante)

## Fase 2 — Domínio: bindings e serviços
- [ ] `SystemTenant`, `UserSystem`, `UserSystemProfile` (+ IDs)
- [ ] `TenantConsistencyValidator`
- [ ] `AccessValidator` — cascata completa de status (3.4)
- [ ] `PlatformAdminPolicy`, `ProfileUniquenessPolicy`
- [ ] `PasswordResetToken`, `ResetTokenValue`
- [ ] Testes unitários de todos os serviços de domínio

## Fase 3 — Persistência
- [ ] Migrations V1–V11 (seção 4)
- [ ] `BaseJpaEntity`, `AuditableJpaEntity`
- [ ] Entidades JPA de todas as tabelas
- [ ] `*JpaRepository` (Spring Data)
- [ ] `AuthMapper` — domínio ↔ entidade, para todos os agregados
- [ ] `*RepositoryImpl` implementando as portas out
- [ ] `TsidGenerator`, `TsidNodeResolver`
- [ ] Testes de integração com Testcontainers
- [ ] Testes de isolamento da seção 8.3 (incluindo violação via SQL direto)

## Fase 4 — Aplicação: administração
- [ ] Use cases de tenant, sistema, perfil, usuário e vínculos
- [ ] Validação de unicidade de perfil por sistema
- [ ] Validação de consistência de tenant em todo vínculo
- [ ] Envio de e-mail de boas-vindas e de reset (porta `EmailSenderPort`)
- [ ] Testes unitários com Mockito

## Fase 5 — Aplicação: autenticação
- [ ] `AuthenticateUserUseCase` — resolve por username **ou** e-mail, dentro do tenant
- [ ] `AuthorizeUserUseCase` — retorna os códigos de perfil ativos
- [ ] `AuthenticatePlatformAdminUseCase`
- [ ] `ResetPasswordUseCase`
- [ ] Falha sempre genérica; sem vazar existência de usuário
- [ ] Testes cobrindo cada nível da cascata de status

## Fase 6 — Segurança e OAuth2
- [ ] `SecurityConfig` — filter chains separados para `/oauth2/**`, `/admin/api/**` e `/login`
- [ ] `AuthorizationServerConfig` — settings, PKCE obrigatório, TTLs
- [ ] `RegisteredClientRepository` customizado sobre `system`
- [ ] `JdbcOAuth2AuthorizationService`
- [ ] Geração/carregamento de chave RSA + `JWKSource` + endpoint JWKS
- [ ] `CustomAuthenticationProvider` (usuário) e provider de platform admin
- [ ] `JwtTokenCustomizer` — claims da seção 7.2
- [ ] Rate limiting e bloqueio por tentativas
- [ ] Testes de emissão e validação de token

## Fase 7 — Tela de login
- [ ] Login em Thymeleaf, responsivo
- [ ] Branding por tenant (nome/logo resolvidos pelo `client_id`)
- [ ] Fluxo de "esqueci minha senha"
- [ ] Tela de consentimento (se houver client de terceiro)
- [ ] Mensagens de erro genéricas na UI
- [ ] Testes com MockMvc

## Fase 8 — API administrativa
- [ ] Controllers da seção 9
- [ ] DTOs de request/response com Bean Validation
- [ ] `GlobalExceptionHandler`
- [ ] OpenAPI/Swagger
- [ ] CORS para o console Angular
- [ ] Testes de integração dos endpoints

## Fase 9 — Console Angular
- [ ] Projeto Angular + cliente OAuth2 PKCE (`angular-oauth2-oidc`)
- [ ] Login via redirect para o auth server
- [ ] CRUD de tenants, sistemas, perfis, usuários
- [ ] Tela de vínculos (usuário → sistemas → perfis)
- [ ] Guard de rota exigindo platform admin
- [ ] Build de produção servido por nginx

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

- Item "Testes ArchUnit das regras da seção 5.1" (Fase 10) já tem um teste inicial em
  `ArchitectureTest` desde a Fase 0. Usa `allowEmptyShould(true)` para não quebrar a suíte
  enquanto os pacotes `application`/`adapter` ainda não têm código — cada regra passa a
  valer de fato (e falharia de verdade) assim que houver uma violação real. O item da
  Fase 10 fecha quando a cobertura de regras estiver completa (incluindo as de `adapter`
  não vazar entidade JPA e DTO não entrar em `application`).
