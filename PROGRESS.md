# Progresso — Modernização Frontend Auth Server v2

Espelha os checklists de `docs/PLANO-MODERNIZACAO.md`, fase por fase. Ver também
`docs/GUIA-DE-ESTILO.md` (normativo).

---

## Fase 0 — Baseline

- [x] `npm install` e confirmar que `ng build` e `ng test` passam **hoje**, antes de tocar
      em qualquer coisa. Registrar tamanho do bundle inicial de produção.
      Bundle inicial: 399.95 kB raw / 99.94 kB transfer. Suíte: 69 testes, todos verdes
      (Karma/Jasmine, `--browsers=ChromeHeadless`).
- [x] Confirmar smoke test manual do login + PKCE do console contra o backend
      (`mvn spring-boot:run` + `ng serve`), para ter certeza de que a base funciona.
      Confirmado manualmente via browser automation — login como platform admin
      (`admin`/`admin@example.com`), redirect PKCE completo até `/console`.
- [x] Commit de baseline — ponto de retorno seguro.

---

## Fase 1 — Angular 19 → 20

- [ ] `ng update @angular/core@20 @angular/cli@20`
- [ ] `ng update angular-oauth2-oidc@20`
- [ ] Migração do builder: `@angular-devkit/build-angular` → `@angular/build`.
- [ ] Rodar as migrations automáticas de `inject()` e signals oferecidas pelo `ng update`.
- [ ] `ng build` + `ng test` verdes.

---

## Fase 2 — Angular 20 → 21 + zoneless

- [ ] `ng update @angular/core@21 @angular/cli@21`
- [ ] `ng update angular-oauth2-oidc@21`
- [ ] Remover `zone.js`.
- [ ] Remover `@angular/platform-browser-dynamic`.
- [ ] Renomear componentes para o padrão sem sufixo (opcional nesta fase).
- [ ] `ng build` verde.
- [ ] Smoke test manual obrigatório do fluxo PKCE completo.

---

## Fase 3 — Migração do runner de testes para Vitest

- [ ] Remover Karma/Jasmine.
- [ ] Adicionar `vitest` + `jsdom`; `angular.json` → `@angular/build:unit-test`.
- [ ] Converter os 20 specs (Jasmine → Vitest).
- [ ] Trocar `HttpClientTestingModule` por `provideHttpClientTesting()`.
- [ ] `ng test` verde com a mesma quantidade de testes de antes.

---

## Fase 4 — Fundação de design

- [ ] Instalar `primeng@21`, `@angular/cdk@21`, `@primeuix/themes`, `primeflex`, `primeicons`.
- [ ] Dev: `prettier`.
- [ ] `styles.css` → `styles.scss` + config em `angular.json`.
- [ ] Criar `src/assets/scss/**` (seção 7.1 do guia).
- [ ] Criar `core/config/providers/primeng.provider.ts` com `AuthServerPreset`.
- [ ] Criar `i18n/primeng-pt.ts` + `LOCALE_ID: 'pt-BR'`.
- [ ] Providers globais: `MessageService`, `ConfirmationService`, `DialogService`.
- [ ] Elevar budgets em `angular.json`.
- [ ] Verificar tema claro e escuro.

---

## Fase 5 — Estrutura compartilhada

- [ ] `features/` → `pages/`.
- [ ] Portar `ListBase`, `FormBase`, `LayoutBasePages`, `FormLabel`, `LoadingOverlay`,
      wrapper de `MessageService`, `Toast`.
- [ ] Criar `StatusTag` e `CopyField`.
- [ ] Criar `core/layout/public-layout` e `core/layout/console-layout`.
- [ ] Reestruturar `app.routes.ts` (pública e `/console`, lazy).
- [ ] `ng build` + `ng test` verdes.

---

## Fase 6 — Telas públicas

- [ ] `/login`.
- [ ] `/esqueci-senha`.
- [ ] `/reset-password`.
- [ ] `/consent`.
- [ ] Estados de carregando/erro/sucesso em todas.
- [ ] Smoke test manual de cada tela contra o backend real.

---

## Fase 7 — Console

- [ ] Shell (topbar + sidebar).
- [ ] Dashboard útil + corrigir bug de sessão expirada.
- [ ] CRUDs com `p-table` + `FormBase`, paginação server-side, `StatusTag`, confirmação:
  - [ ] Tenants
  - [ ] Sistemas (redirect URIs, rotação de secret com `CopyField`)
  - [ ] Perfis
  - [ ] Usuários (reset de senha administrativo)
  - [ ] Vínculos
  - [ ] Platform Admins (tela nova)
- [ ] Dark mode com persistência de preferência.

---

## Fase 8 — Fechamento

- [ ] `ng build --configuration production` dentro dos budgets.
- [ ] Smoke test com nginx real.
- [ ] Roteiro E2E manual completo.
- [ ] Revisar acessibilidade.
- [ ] Atualizar `README.md` do frontend.
- [ ] Atualizar backend: `03-subir-ambiente-local.md` e `PROGRESS.md`.

---

## Notas

- **Ambiente local tem Postgres nativo do Windows disputando a porta 5432** com o
  `docker-compose.yml` do `auth_api_v2` — os dois processos escutam em `0.0.0.0:5432` e o
  backend às vezes conecta no nativo (sem o usuário `authserver`) em vez do container.
  Contorno usado no smoke test: `docker-compose.override.yml` local (não commitado) mapeando
  `15432:5432` + `DB_URL=jdbc:postgresql://localhost:15432/authserver`. Não é um problema do
  frontend, mas quem for rodar o backend local nesta máquina vai esbarrar nisso.
- **Bug encontrado no backend (`auth_api_v2`): `authserver.frontend.login-url` não tem
  override no profile `dev`.** Cai no default `/login` de `application.yml`, relativo à
  própria API (`:8080`), não ao frontend (`:4200`) — o `SpaLoginAuthenticationEntryPoint`
  redireciona para uma rota inexistente na API (404 Whitelabel). Contorno usado no smoke
  test: `LOGIN_URL=http://localhost:4200/login` como env var. Vale reportar/corrigir no
  repo do backend (`application-dev.yml`), fora do escopo desta skill.
- **Doc do backend (`03-subir-ambiente-local.md`) sugere seed com `admin@localhost`, mas o
  `Email` VO do domínio exige TLD** (`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`) —
  `admin@localhost` nunca passa em `Email.of(...)`. Usei `admin@example.com` no seed manual
  para o smoke test. Também vale reportar no repo do backend.
- Platform admin de teste seedado manualmente em `auth_api_v2` (ambiente local): usuário
  `admin`, e-mail `admin@example.com`, senha `TrocarEssaSenha123`. Fica registrado aqui para
  reuso nas próximas rodadas de smoke test (Fases 2, 6, 7, 8) — não recriar do zero toda vez.
