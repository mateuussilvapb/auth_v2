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

- [x] `/login` — `p-inputtext`/`p-password`, branding do tenant no cabeçalho (`.tenant-logo`,
      guia 6.4), erro genérico exibido **literalmente** como recebido do backend (testes
      cobrindo dois cenários de causa diferente com a mesma mensagem, guia 6.1).
- [x] `/esqueci-senha` — resposta idêntica exista ou não o e-mail (mensagem fixa "Se o
      e-mail estiver cadastrado, enviaremos as instruções.", guia 6.2). Teste parametrizado
      cobrindo sucesso e 404 com a mesma asserção de UI.
- [x] `/reset-password` — `p-password` com `[feedback]` (força + regras) e `minlength(8)`.
- [x] `/consent` — lista de escopos em mono, ações **Autorizar/Negar**. Não existe endpoint
      de negação no backend (só grava consentimento); "Negar" apenas encerra o fluxo local
      sem chamar a API nem retomar `/oauth2/authorize` — registrado como decisão de produto
      não coberta pelo plano.
- [x] Estados de carregando/erro/sucesso desenhados em todas (via `[loading]` do `p-button`
      e os `@if` de sucesso/erro já existentes).
- [x] Todas as quatro telas embrulhadas no `PublicLayout` (`app.routes.ts` — rota `''` com
      `children`); CSS antigo de cada tela removido.
- [x] **Smoke test manual completo contra o backend real**, incluindo o cenário de
      `/consent` com `System.thirdParty = true` (nunca testado manualmente antes, conforme
      nota do `PROGRESS.md` do backend): criado tenant `acme` + sistema `CRM_ACME`
      (thirdParty, público) + usuário `joao_silva` vinculado, via o próprio console (CRUD
      antigo, ainda funcional). Validado: branding do tenant no login, mensagem de erro
      idêntica para senha errada, `/esqueci-senha` com a mesma mensagem para e-mail
      existente e inexistente, `/consent` renderizando escopos reais e "Negar" funcionando,
      `/reset-password` com link inválido e com token (força de senha visível). Login com
      sucesso navegou corretamente para `/oauth2/authorize` (o 400 ali é esperado — a URL de
      teste não tinha os parâmetros OAuth2 completos, já que não veio de um redirect real).

---

## Fase 7 — Console

- [x] Shell (topbar + sidebar). Smoke test manual confirmado em 2026-08-29 (ver nota abaixo)
      — login como platform admin, seleção de tenant obrigatória, sidebar reagindo ao tenant
      selecionado, troca de tenant pelo header e logout, tudo funcionando no navegador real
      contra o backend local. `npm run build` e `npm test` verdes.
      Escopo entregue, além do item original do plano (decisão de produto 2026-08-29, pedida
      explicitamente pelo usuário fora da ordem do plano):
      - `TenantContextService` (`core/services/tenant-context.service.ts`) — contexto de
        tenant selecionado, persistido em `localStorage` cifrado com AES-GCM
        (`core/util/crypto.ts`), chave derivada (SHA-256) do access token corrente. Grava só
        `{ id, code }`, nunca o objeto completo.
      - `tenantContextGuard` (`core/guards/tenant-context.guard.ts`) — aplica-se só a platform
        admin (claim `platform_admin` do JWT). Sem tenant algum cadastrado, redireciona para
        `/console/tenants/novo`; com tenants existentes e nenhum selecionado, redireciona
        para `/console/selecionar-tenant`. Gestão de tenants (`tenants`, `tenants/novo`,
        `tenants/:id/editar`) fica fora do guard de propósito — é a única área navegável
        antes do primeiro tenant existir.
      - Tela nova `/console/selecionar-tenant` (`pages/console/tenant-selection`) — lista
        paginada (`p-table` lazy) de tenants, seleciona e retoma a navegação original
        (`returnUrl`) ou vai ao dashboard.
      - Todo login (`ConsoleCallback`) limpa o contexto de tenant persistido — seleção é
        exigida a cada sessão nova, mesmo com um valor válido já em `localStorage`.
      - Logout (`Topbar.logout`) limpa o contexto de tenant.
      - Topbar exibe o tenant selecionado (código, fonte mono), clicável — troca limpa o
        contexto e navega para `/console/selecionar-tenant`; em rotas marcadas
        `data: { critical: true }` (formulários de criação/edição), pede confirmação via
        `ConfirmationService` antes de trocar. Novo componente compartilhado `ConfirmDialog`
        (`shared/components/confirm-dialog`) registrado no `ConsoleLayout`.
      - Sidebar usa o tenant selecionado para montar os links de Sistemas/Usuários
        (`/console/tenants/:id/systems`, `/console/tenants/:id/users`) — some quando nenhum
        tenant está selecionado. **Perfis, Vínculos e Platform Admins ainda não entraram na
        sidebar**: não têm rota independente de um sistema/usuário/tela específicos ainda —
        entram junto da migração dos respectivos CRUDs (próximos itens desta fase).
      - 143 testes (34 novos/atualizados nesta rodada) cobrindo os itens acima.
      - Sidebar oculta na tela `/console/selecionar-tenant` (pedido explícito do usuário,
        2026-08-29): sem tenant selecionado nenhum item do menu tem contexto pra navegar.
        `ConsoleLayout` lê `route.data['hideSidebar']` (mesmo padrão do `isCriticalScreen` do
        `Topbar`) e alterna `<app-sidebar>` e a classe `.layout-main-container--no-sidebar`
        (remove o `padding-left` reservado à sidebar). 2 testes novos.
- [x] Dashboard útil + corrigir bug de sessão expirada. Substituído o placeholder de claims
      cruas por três contadores (tenants totais, sistemas e usuários do tenant selecionado —
      `Page.totalElements` de uma página `size=1`, cada contador com seu próprio estado de
      erro independente) e atalhos para `/console/tenants`, `.../systems` e `.../users` do
      tenant corrente. Bug de sessão expirada (guia 6.5, `api/PROGRESS.md` 2026-08-29)
      corrigido: `ConsoleDashboard` deriva o estado de autenticação de
      `ConsoleAuthService.isAuthenticated()` (que internamente compara `expires_at`), checado
      ao entrar na tela e reavaliado a cada 30s via `setInterval` (não é hack de change
      detection — grava em signal, que o zoneless já reage sozinho) enquanto o admin fica
      parado nela sem navegar. Sessão expirada → tela dedicada ("Sessão expirada" + botão
      "Entrar novamente" que chama `consoleAuth.login()`) em vez de continuar mostrando
      "Logado como X" com claims obsoletas. 3 testes novos (autenticado com contagens, sessão
      expirada em vez de claims, erro isolado por contador). `npm run build` e `npm test`
      verdes (147/147).
      **Nota de comportamento**: `angular-oauth2-oidc` tem `clockSkewInSec` default de 600s
      (10 min) — `hasValidAccessToken()` só retorna `false` uns 10 minutos depois do
      `expires_at` real, não no instante exato da expiração. Confirmado manualmente no
      navegador (`sessionStorage.expires_at` forçado para o passado): com token expirado há 1
      minuto o dashboard ainda se considerava autenticado; só com expiração de ~20 minutos a
      tela de sessão expirada apareceu. Esse skew já valia antes desta rodada para o
      `consoleAuthGuard` (mesmo método) — não é regressão, mas vale saber que "sessão
      expirada" na prática significa "expirada há mais de ~10 minutos", não "expirada agora".
      Smoke test manual completo: dashboard com contagens reais (1 tenant, 0
      sistemas/usuários) → atalho "Sistemas" navegou para
      `/console/tenants/<id>/systems` corretamente → sessão expirada simulada via
      `sessionStorage` mostrou a tela dedicada sem navegação → "Entrar novamente" disparou
      `initCodeFlow` de verdade (sessão HTTP do backend ainda viva, reautenticou e voltou
      para `/console/selecionar-tenant`, já que `ConsoleCallback` limpa o contexto de tenant
      em todo login, comportamento existente).
- [ ] CRUDs com `p-table` + `FormBase`, paginação server-side, `StatusTag`, confirmação:
  - [x] Tenants. `TenantList` (`p-table` lazy, paginação server-side, `StatusTag`,
        skeleton de carregamento via `#loadingbody` + `p-skeleton`, estado vazio com ação
        primária, estado de erro com "Tentar novamente" — os três estados obrigatórios da
        seção 5.3 do guia) e `TenantForm` (primeiro consumidor real de `FormBase`/
        `FormLabel`/Reactive Forms no console). Ativar/desativar: só desativar pede
        confirmação via `ConfirmationService` nomeando o tenant e a consequência (guia 5.1);
        ativar é direto (não é destrutivo). `code` fica desabilitado (não escondido, guia
        5.2) em modo edição — é imutável após a criação.
        **Extensão em `FormBase`** (`shared/components/form-base/form-base.ts`): a nota de
        compatibilidade da Fase 5 apontava duas saídas — renomear rotas para
        `cadastro`/`edicao` ou passar o modo explicitamente. Rotas (`novo`/`editar`) não
        foram renomeadas para não quebrar `tenant-context.guard`, os testes existentes do
        `Topbar` e as demais telas do console ainda não migradas — em vez disso,
        `isEditMode`/`isCreateMode`/`isViewMode` agora também leem `route.data['formMode']`
        (prioridade entre dialogData/data/URL), declarado em `app.routes.ts` nas rotas
        `tenants/novo` (`formMode: 'cadastro'`) e `tenants/:id/editar`
        (`formMode: 'edicao'`). Sistemas/Perfis/Usuários/Vínculos podem reusar o mesmo padrão
        quando forem migrados.
        16 testes novos (`TenantList`, `TenantForm`, `FormBase`). `npm run build` e
        `npm test` verdes (151/151).
        Smoke test manual completo: listagem com dado real (`StatusTag` "Ativo"/"Inativo"),
        confirmação de desativação nomeando o tenant certo, criação de um tenant de teste
        (toast de sucesso), edição com `código` visivelmente desabilitado e valor carregado,
        ativar/desativar de verdade sem confirmação/com confirmação respectivamente. Sem
        erros no console.
        **Achado à parte, não é bug desta rodada**: com o `access_token` expirado há mais de
        ~9 minutos mas menos de 10 (dentro do `clockSkewInSec` default da
        `angular-oauth2-oidc`, ver nota do item "Dashboard" acima), `consoleAuthGuard` deixa
        passar mas a API já rejeita com 401 de verdade — a tela (`TenantList`) tratou isso
        corretamente como o estado de erro com "Tentar novamente" (não um crash), mas o
        ideal seria essa janela de ~10 min não existir. Não mexi no `clockSkewInSec` porque
        afeta `consoleAuthGuard` e todo o app, não só esta tela — decisão de produto fora do
        escopo deste item.
  - [x] Sistemas (redirect URIs, rotação de secret com `CopyField`). `SystemList` (mesmo
        padrão de `TenantList`: `p-table` lazy, paginação server-side, `StatusTag`, três
        estados). `SystemForm` cobre criação (rota, `/console/tenants/:tenantId/systems/novo`)
        **e** edição (diálogo, aberto por `SystemList.edit()` via `DialogService.open(...)`
        passando `dialogData.valoresIniciais`).
        **Decisão de arquitetura — edição por diálogo, não por rota**: a admin-api **não tem**
        `GET /admin/api/v1/systems/{id}`, só `GET .../tenants/{tenantId}/systems` (lista) —
        confirmado lendo `SystemController.java`. Existe até `ManageSystemUseCase.getSystem`
        no backend, só não está exposto no controller. Como `SystemList` já tem o
        `SystemResponse` completo em memória ao clicar "Editar", abrir como diálogo
        (`FormBase` já suporta esse modo desde a Fase 5) evita depender de um endpoint que não
        existe, sem tocar no backend. Só `name` é editável depois da criação
        (`UpdateSystemRequest` do backend só aceita isso) — `clientId`/`publicClient`/
        `thirdParty` ficam desabilitados (visíveis, guia 5.2) no diálogo.
        **`clientSecret` nunca digitado pelo admin** (guia 5.5): novo util
        `core/util/secret.ts#generateClientSecret()` gera 32 bytes aleatórios
        (`crypto.getRandomValues`) client-side, tanto na criação (client confidencial) quanto
        na rotação — exibido uma única vez via `CopyField` com o aviso "Guarde-o agora",
        nunca persistido em storage. `AdminApiService` ganhou `rotateSecret`/
        `removeRedirectUri` (só existia `addRedirectUri`). Remover redirect URI e rotacionar
        secret pedem confirmação (guia 5.1 lista "rotacionar secret" como ação destrutiva).
        27 testes novos (`SystemList`, `SystemForm`, `generateClientSecret`,
        `AdminApiService`). `npm run build` e `npm test` verdes (165/165).
        Smoke test manual: criação de sistema confidencial com secret gerado e exibido
        (`CopyField`, botão copiar funcional — confirmado via toast "Valor copiado", embora o
        ícone `pi-copy` não renderize visualmente, bug cosmético pré-existente do `CopyField`
        da Fase 5, fora do escopo deste item), listagem com `Tipo`/`Status` corretos, diálogo
        de edição abrindo com campos certos desabilitados, confirmação de rotação de secret e
        de remoção de redirect URI nomeando o sistema/valor certos.
        **Bug real encontrado no backend, fora do escopo desta skill**: **todo** `save()` em
        um `System` **já existente** falha com 500 genérico — reproduzido com `updateSystem`
        (só o nome), `addRedirectUri`, `rotateSecret` e `updateSystemStatus`, todos via
        `curl`/`fetch` direto (não é bug do frontend). Só a criação inicial
        (`createSystem`, primeiro insert) funciona. Suspeita, sem confirmar: `SystemEntity`
        (`api/.../adapter/out/persistence/entity/SystemEntity.java`) mapeia `redirectUris`
        como `@OneToMany(cascade = ALL, orphanRemoval = true)` — padrão clássico de
        `TransientPropertyValueException`/violação de constraint no Hibernate quando o mapper
        substitui a coleção inteira em vez de mutar in-place num update. Tenants (mesma
        sessão, item anterior) não tem esse problema — o bug parece específico de `System`.
        Vale abrir como item no `PROGRESS.md`/backlog do backend; não investiguei mais fundo
        nem tentei corrigir (fora do escopo desta skill, que é só frontend).
  - [x] Perfis. Mesmo padrão de Tenants/Sistemas (`p-table`, `StatusTag`, `LayoutBasePages`,
        confirmação ao desativar), mas sem paginação — `GET .../profiles` não pagina no
        backend (poucos perfis por sistema na prática). `ProfileForm` usa **rota** (não
        diálogo) para criação e edição, diferente de `SystemForm`: aqui existe
        `GET /admin/api/v1/systems/{systemId}/profiles/{id}` de verdade
        (`SystemProfileController.get`), então `AdminApiService` ganhou `getProfile` e a
        edição carrega fresco por id, igual a `TenantForm`. `code` é único por sistema
        (`UNIQUE (systemId, code)`) e imutável — desabilitado (não escondido) em modo edição,
        só `description` é aceito por `UpdateSystemProfileRequest`.
        22 testes novos (`ProfileList`, `ProfileForm`, `AdminApiService.getProfile`).
        `npm run build` e `npm test` verdes (173/173).
        Smoke test manual: criação de perfil, edição carregando `GET .../profiles/:id` com
        `código` desabilitado, **update funcionando** (diferente do bug de `System` do item
        anterior — confirma que o bug de `save()` é específico da entidade `System`, não
        sistêmico), confirmação de desativação nomeando o perfil certo. Sem erros no console.
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

- **`/esqueci-senha` não enviou e-mail via MailHog mesmo para o e-mail existente**
  (`joao@example.com`, tenant `acme`, sistema `CRM_ACME`) durante o smoke test da Fase 6 —
  só chegou o e-mail de boas-vindas da criação do usuário. A resposta da API foi 200 em
  ambos os casos (existente/inexistente) e a UI mostrou a mensagem genérica corretamente
  nos dois — o comportamento **visível ao usuário está correto** (não há enumeração). Não
  investiguei a fundo (pode ser um no-op silencioso por falta de vínculo `SystemTenant`, o
  que também seria um comportamento anti-enumeração válido) — vale conferir no backend se
  for reportado como bug real de não-envio.

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
- **Bug corrigido: "Sair" não fazia nada visível.** O client do console usa `scope: 'profile'`
  sem `openid` (decisão de design documentada em `ConsoleAuthService` — sem `id_token`), mas
  `oauthService.logOut()` sempre tentava RP-initiated logout no `end_session_endpoint` do
  backend (`/connect/logout`), que **responde 400 Bad Request sem `id_token_hint`**
  (confirmado com `curl` contra o backend local) — o usuário via a página quebrar em
  silêncio, sem nenhum feedback. Corrigido: `ConsoleAuthService.logout()` agora chama
  `oauthService.logOut(true)` (só limpa tokens localmente, sem tentar o endpoint quebrado); o
  `Topbar.logout()` força `window.location.href = '/console'` depois, porque
  `router.navigate` não reavalia guards numa navegação para a mesma URL — sem isso o usuário
  ficaria "deslogado" mas ainda vendo a tela protegida até a próxima navegação manual.
- **Causa raiz real do "Sair" corrigida (a correção acima só tratou o sintoma).** Mesmo com o
  fix acima, o usuário relatou continuar caindo em `/console/selecionar-tenant` após clicar em
  Sair. Motivo: limpar só os tokens OAuth no `localStorage` nunca invalidava a **sessão HTTP**
  do backend (cookie `JSESSIONID`) — o backend não tinha nenhum `.logout(...)` configurado em
  nenhum `SecurityFilterChain` (`SecurityConfig.java`, confirmado por busca — zero ocorrências
  de "logout"). Com a sessão ainda viva, o próximo `GET /oauth2/authorize` reautenticava
  **silenciosamente** (via `HttpSessionSecurityContextRepository`) e devolvia um novo
  `authorization_code` sem pedir login — o usuário via o Sair "não fazer nada" porque era
  deslogado e relogado no mesmo round-trip. Corrigido em duas pontas:
  - Backend (`api/`): novo `POST /api/auth/logout` (`AuthController.java`) que invalida a
    `HttpSession` via `session.invalidate()`. Teste de integração
    (`AuthControllerIntegrationTest.deveInvalidarSessaoAoDeslogarImpedindoReautenticacaoSilenciosaNoAuthorize`)
    reproduz o cenário completo: login → logout → `GET /oauth2/authorize` com a mesma sessão
    agora redireciona para `/login` em vez de reautenticar. 499/499 testes do backend verdes.
  - Frontend: `AuthApiService.logout()` chama o endpoint novo (`withCredentials: true`, mesmo
    padrão dos outros métodos da classe); `ConsoleAuthService.logout()` aguarda essa chamada
    (best-effort — segue limpando os tokens locais mesmo se a chamada falhar, ex. rede fora)
    antes de `oauthService.logOut(true)`. `Topbar.logout()` agora é assíncrono e só recarrega
    a página depois que o backend confirma.
  - **Armadilha**: ao escrever o teste de `ConsoleAuthService.logout()`, sobrescrevi sem
    querer um spec já existente (`console-auth.service.spec.ts` já tinha 3 testes cobrindo
    `login()`/discovery/`isAuthenticated()`/`getAccessToken()`) em vez de editá-lo — perdi
    cobertura por um instante (143→142 testes) até notar a divergência e mesclar os dois
    conjuntos de testes no mesmo arquivo. 145/145 testes do frontend verdes no final.
- **2026-08-29, smoke test do item "Shell (topbar + sidebar)" concluído**: o seed antigo
  (`admin`/`admin@example.com`) tinha sumido do volume do Postgres (só restava um platform
  admin real, `mssousa`, sem senha conhecida). Reseedado via SQL direto (mesmo procedimento
  do passo 4 de `api/docs/03-subir-ambiente-local.md`), `admin`/`TrocarEssaSenha123`.
  **Armadilha nova**: ao montar o `INSERT`/`UPDATE` do hash BCrypt via PowerShell com string
  **entre aspas duplas**, o `$` do hash (`$2a$12$...`) é interpretado como início de variável
  do PowerShell e expandido para vazio, corrompendo o hash silenciosamente (o `INSERT`/
  `UPDATE` não dá erro nenhum — só o login falha depois com "Invalid credentials"). Corrigido
  usando aspas simples no PowerShell (`'...'`, literal, sem interpolação) tanto para o
  comando quanto para o valor SQL. Vale para qualquer valor com `$` inserido via `docker exec
  psql -c` a partir do PowerShell. Fluxo completo confirmado no navegador (extensão Claude in
  Chrome): login → `/console/selecionar-tenant` (sidebar oculta) → seleção → sidebar com
  Tenants/Sistemas/Usuários reagindo ao tenant → clique no código do tenant na topbar troca o
  contexto e volta para seleção (rota não crítica, sem diálogo de confirmação, como esperado)
  → Sair limpa a sessão e volta para `/login` com um novo desafio PKCE (sem reautenticação
  silenciosa). Sem erros no console do navegador.

- **Reunificado em `sistemas/auth_v2` em 2026-08-29**, no mesmo monorepo do backend (pastas
  `api/` e `web/`, seguindo o padrão de `sistema_promissorias`). Histórico deste repositório
  (`auth_frontend_v2`) preservado via `git subtree add --prefix=web`. Referência ao backend
  na skill `modernize-auth-frontend-v2` atualizada de `auth_api_v2` para `../api`.
