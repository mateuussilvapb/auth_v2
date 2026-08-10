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

- [x] `ng update @angular/core@20 @angular/cli@20`
- [x] `ng update angular-oauth2-oidc@20`
- [x] Migração do builder: `@angular-devkit/build-angular` → `@angular/build`.
      `ng update @angular/cli --name use-application-builder` não rodou (o schematic runner
      sempre busca a CLI mais recente do registro — hoje 22.x — que exige Node ≥22.22.3;
      a máquina tem 22.16.0). Migração feita manualmente: instalado `@angular/build@20.3.33`
      e trocados os builders em `angular.json` (`build`, `serve`, `extract-i18n`) para
      `@angular/build:*`. `test` continua em `@angular-devkit/build-angular:karma` até a
      Fase 3 (Vitest via `@angular/build:unit-test`).
- [x] Rodar as migrations automáticas de `inject()` e signals oferecidas pelo `ng update`.
      Nenhuma mudança de código foi necessária (projeto já usava `inject()`/`signal()`).
- [x] `ng build` + `ng test` verdes. Bundle: 409.65 kB raw / 102.74 kB transfer (leve alta
      vs. baseline, esperado). 69/69 testes.

---

## Fase 2 — Angular 20 → 21 + zoneless

- [x] `ng update @angular/core@21 @angular/cli@21`. Trouxe automaticamente a migration
      opcional de control flow (`*ngIf`/`*ngFor` → `@if`/`@for`) em 14 componentes.
- [x] `ng update angular-oauth2-oidc@21`
- [x] Remover `zone.js` do runtime: tirado de `polyfills` em `angular.json` (target `build`)
      e trocado `provideZoneChangeDetection({ eventCoalescing: true })` por
      `provideBrowserGlobalErrorListeners()` em `app.config.ts`, igual à referência.
      **`zone.js` continua como dependência e nos polyfills do target `test`** — o Karma
      ainda depende dele; sai de vez na Fase 3 (migração para Vitest).
- [x] Remover `@angular/platform-browser-dynamic` (não era importado em lugar nenhum).
- [ ] Renomear componentes para o padrão sem sufixo — adiado para a Fase 5 (obrigatório lá,
      opcional aqui; não fiz para não misturar rename com upgrade de versão no diff).
- [x] `ng build` verde. Bundle: 379.17 kB raw / 92.24 kB transfer (caiu vs. Fase 1 — sem o
      polyfill do zone.js). `ng test` também verde, 69/69.
- [x] Smoke test manual obrigatório do fluxo PKCE completo — **sem regressão sob zoneless**.
      Login como platform admin, redirect PKCE completo, navegação interna autenticada
      (`/console/tenants`, chamada à admin-api, paginação renderizada) tudo funcionando sem
      Zone.js. Sem erros no console do navegador.

---

## Fase 3 — Migração do runner de testes para Vitest

- [x] Remover Karma/Jasmine (`karma`, `karma-*`, `jasmine-core`, `@types/jasmine`) e `zone.js`
      (agora fora do runtime **e** dos testes — zoneless de ponta a ponta).
      `@angular-devkit/build-angular` também saiu (só existia para o builder `karma`).
- [x] Adicionar `vitest@^4.1.10` + `jsdom`; `angular.json` → `@angular/build:unit-test`
      (config mínima, igual à referência — só o builder, sem opções explícitas; runner
      `vitest` e `buildTarget` são os defaults). `tsconfig.spec.json` → `types: ["vitest/globals"]`.
- [x] Converter os 18 specs (69 testes) de Jasmine para Vitest. Conversão mecânica via
      sed + ajustes manuais nos casos multi-linha: `jasmine.createSpy('x')` → `vi.fn()`,
      `.and.returnValue(` → `.mockReturnValue(`, `.and.resolveTo(` → `.mockResolvedValue(`,
      `jasmine.Spy` → `Mock` (import `type { Mock } from 'vitest'`), `jasmine.objectContaining`
      → `expect.objectContaining`, `spyOn(` → `vi.spyOn(`.
- [x] Trocar `HttpClientTestingModule` por `provideHttpClient()` + `provideHttpClientTesting()`
      nos 5 arquivos que usavam (login, consent, forgot-password, reset-password,
      admin-api.service).
- [x] `ng test` verde, **69/69** — mesma quantidade de testes de antes (a nota do plano
      dizia "20 specs"; o número real medido na Fase 0 já era 69 testes em 18 arquivos de
      spec — nenhum teste foi perdido na conversão).

---

## Fase 4 — Fundação de design

- [x] Instalar `primeng@21.1.9`, `@angular/cdk@21.2.14`, `@primeuix/themes@2.0.3`,
      `primeflex@4.0.0`, `primeicons@7.0.0` (mesmas versões da referência).
- [x] Dev: `prettier` (instalado na Fase 3 junto do vitest; sem config própria ainda —
      revisitar se o time quiser regras específicas).
- [x] `styles.css` → `styles.scss` (agregador `@use 'assets/scss/base'`) + `angular.json`
      (`inlineStyleLanguage: scss`, `styles: [styles.scss, primeflex.css]`, schematic de
      componente default para `style: scss`).
- [x] Criado `src/assets/scss/**` (seção 7.1 do guia): `base.scss`, `base/_reset.scss`,
      `_typography.scss` (Manrope + JetBrains Mono via Google Fonts, escala tipográfica da
      seção 3), `utils/_variables.scss` (índigo/raio/transição, seção 2.1/4 — **sem**
      gradiente, divergência deliberada da referência), `utils/_mixins.scss`
      (`focused()`/`focused-inset()`), `utils/_topbar.scss`, `utils/_menu.scss`,
      `utils/_content.scss` (inclui `.public-layout`/`.public-layout-card` para o shell das
      telas públicas). `primeNG/` ainda sem arquivos — nada para sobrescrever até as telas
      reais da Fase 6/7 revelarem necessidade.
- [x] Criado `core/config/providers/primeng.provider.ts` com `AuthServerPreset` — paleta
      índigo da seção 2.4 do guia, `darkModeSelector: '.app-dark'`, `ripple: false`.
- [x] Criado `i18n/primeng-pt.ts` (tradução completa) + `LOCALE_ID: 'pt-BR'` +
      `registerLocaleData(localePt)` em `app.config.ts`.
- [x] Providers globais registrados em `app.config.ts`: `PRIMENG_PROVIDER`, `MessageService`,
      `ConfirmationService`, `DialogService`.
- [x] Budgets elevados para 1MB/1.5MB (igual à referência). Build de produção: 1.06MB raw —
      dentro do budget de erro, warning esperado (registrado no plano).
- [x] Tema claro e escuro verificados com `p-button` (`severity` default e `danger`) inserido
      temporariamente em `/login` e revertido depois: `--p-primary-color` trocou de
      `#4f46e5` (light, `primary.600`) para `#818cf8` (dark, `primary.400`) ao alternar a
      classe `.app-dark`; `body` escureceu para `#0b1020`. Preset funcionando; nenhuma tela
      real foi tocada (o CSS antigo do `/login` continua intacto até a Fase 6).

---

## Fase 5 — Estrutura compartilhada

- [x] `features/` → `pages/`, seguindo o padrão da referência: cada componente na sua
      própria pasta `pages/<modulo>/components/<nome>/<nome>.ts` (sem sufixo `Component`,
      arquivo sem `.component.`), console aninhado em `pages/console/<modulo>/`.
      **Desvio deliberado do padrão `core/{models,dtos,services}` por página**:
      `AdminApiService`/`AuthApiService`/`ConsoleAuthService` e os models
      (`admin-api.models.ts`, `auth-api.models.ts`) continuam em `app/core/` (não
      fragmentados por página) — guia 1.1 pede explicitamente para preservá-los sem
      reescrita gratuita, e são consumidos por várias páginas (não fariam sentido
      "pertencendo" a uma única).
- [x] Portadas `ListBase`, `FormBase`, `LayoutBasePages`, `FormLabel`, `LoadingOverlay`,
      wrapper de `MessageService`, `Toast` — sem alteração de contrato, exceto removendo a
      dependência de `ThemeService`/classes de gradiente da referência (não existem neste
      projeto; `LayoutBasePages` usa `severity="primary"` direto). **Nota de compatibilidade
      para Fase 6/7**: `FormBase.isEditMode/isCreateMode/isViewMode` detectam o modo por
      substring da URL (`'cadastro'`/`'edicao'`/`'visualizacao'`, contrato original da
      referência) — as rotas atuais deste projeto usam `'novo'`/`'editar'`, que **não**
      batem com esse contrato. Ao adotar `FormBase` nos formulários reais (Fase 6/7),
      renomear os segmentos de rota para `cadastro`/`edicao` ou passar o modo explicitamente
      via `dialogData`.
- [x] Criados `StatusTag` (mapeamento único de status, guia 2.3) e `CopyField` (mono +
      copiar + máscara opcional, guia 5.5) — específicos deste projeto.
- [x] Criados os dois layouts: `core/layout/console-layout` (topbar + sidebar + LayoutService,
      portados da referência; sidebar só com "Tenants" por ora — Sistemas/Perfis/Usuários/
      Vínculos/Platform Admins entram na Fase 7) e `core/layout/public-layout` (card
      centralizado). **`console-layout` já está ligado às rotas `/console/**`** (smoke test
      manual confirmou navegação, guard e shell funcionando); **`public-layout` foi criado
      mas ainda não ligado às rotas públicas** — as telas antigas (`/login` etc.) têm o
      próprio card com CSS hardcoded, e embrulhá-las agora criaria caixa dupla. Passam a usar
      `public-layout` quando forem reescritas na Fase 6.
- [x] `app.routes.ts` reestruturado em duas árvores lazy (`loadComponent`), com `/console`
      como rota pai (`ConsoleLayout` + `consoleAuthGuard` uma vez só, protegendo toda a
      subárvore) e `children` para cada tela. `/console/callback` fica fora do guard
      (é o próprio destino do fluxo PKCE, ainda sem token).
- [x] `ng build` verde — bundle inicial caiu para 992.62 kB/160.17 kB (lazy loading real:
      cada tela é seu próprio chunk, poucos KB cada). `ng test` verde: **114/114** (69
      preexistentes + 45 novos, cobrindo todo shared/core criado nesta fase).
- [x] Smoke test manual: login + PKCE, guard, navegação aninhada `/console/tenants` dentro
      do novo `ConsoleLayout`, tudo funcionando pós-reestruturação.

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

- **`vi.spyOn(router, 'navigate').mockResolvedValue(true)` sozinho não bastava sob o novo
  runner Vitest/zoneless** em 3 specs (`tenant-form`, `system-form`, `user-form`): o
  `Router` real, configurado com `provideRouter([])` (sem rotas), lançava `NG04002: Cannot
  match any routes` como **unhandled rejection** depois do teste já ter passado — o
  `Test Files`/`Tests` ficavam verdes mas o processo saía com código de erro por causa dos
  "Unhandled Errors". Não identifiquei a origem exata da segunda navegação (a asserção do
  spy já confirma que o componente chamou `router.navigate` corretamente uma vez). Mitigação
  aplicada: trocar `provideRouter([])` por `provideRouter([{ path: '**', component:
  <ComponenteDaTela> }])` nesses 3 specs, dando ao router real uma rota para casar caso
  alguma navegação escape do mock. Resolveu sem alterar a asserção de negócio do teste. Se
  aparecer de novo em specs futuras (Fase 6/7) que usam `spyOn(router, 'navigate')` com
  `provideRouter([])`, aplicar o mesmo padrão.

- **Node.js local (22.16.0) está abaixo do que o `ng update` passou a exigir para rodar
  schematics via CLI temporária (≥22.22.3).** `ng update @angular/core@20`/`@cli@20` e
  `ng update angular-oauth2-oidc@20` funcionaram (a CLI temporária baixada casava com a
  versão alvo pedida). Mas `ng update @angular/cli --name <migration>` sempre busca a
  **última** versão publicada da CLI (hoje 22.x) para rodar a migration, e essa falha no
  Node atual. Isso vai bloquear migrations futuras dependentes desse mecanismo (ex.:
  `control-flow-migration`, `router-current-navigation`, que ficaram como opcionais não
  aplicadas nesta rodada). Se precisar delas, ou atualizar o Node da máquina, ou aplicar a
  mudança manualmente como foi feito com `use-application-builder`.

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
