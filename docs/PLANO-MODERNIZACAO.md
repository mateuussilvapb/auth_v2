# Plano de Modernização — Frontend Auth Server v2

Migração do frontend de Angular 19 / HTML cru para Angular 21 + PrimeNG 21, adotando a
estrutura de `questoes-concurso` como referência arquitetural e a identidade visual
definida em [`GUIA-DE-ESTILO.md`](./GUIA-DE-ESTILO.md).

Formato inspirado no `PROGRESS.md` do backend: fases sequenciais, cada uma verificável
antes da seguinte, com notas registrando decisões e armadilhas encontradas.

---

## 1. Ponto de partida

### 1.1 Estado atual (medido, não estimado)

| | Atual | Alvo |
|---|---|---|
| Angular | 19.2 | 21.2 |
| Builder | `@angular-devkit/build-angular` | `@angular/build` |
| Change detection | Zone.js (`provideZoneChangeDetection`) | Zoneless |
| Testes | Karma + Jasmine (20 specs, 9 usam API do Jasmine) | Vitest + jsdom |
| UI | HTML/CSS cru — `<table>`, `<input>`, CSS por componente | PrimeNG 21 + primeflex |
| Estilo global | `styles.css` **vazio** (1 linha de comentário) | `styles.scss` + `assets/scss/**` |
| Design system | Inexistente | Guia de estilo + preset PrimeNG |
| Layout/shell | Inexistente (dashboard é `<h1>` + 1 link + botão Sair) | Public layout + Console layout |
| TypeScript | 5.7 | 5.9 |
| Auth OIDC | `angular-oauth2-oidc@19` | `angular-oauth2-oidc@21` |

O que **já está bom** e deve ser preservado sem reescrita gratuita:

- `AdminApiService` / `AuthApiService` — cobertura completa da admin-api, IDs corretamente
  tipados como `string`.
- `ConsoleAuthService` + `consoleAuthGuard` — PKCE funcionando, validado manualmente ponta
  a ponta contra o backend real.
- Uso de `signal()` nos componentes — já compatível com zoneless.
- `nginx.conf` — validado com nginx real; duas correções não óbvias embutidas
  (`$http_host`, `requireHttps: 'remoteOnly'`).

### 1.2 Compatibilidade verificada

Consultado no registro npm nesta data — não é suposição:

| Pacote | Versão alvo | Peer dependency | OK? |
|---|---|---|---|
| `@angular/core` | 21.2.19 (`v21-lts`) | — | ✅ |
| `primeng` | 21.1.9 | `@angular/core ^21.0.7`, `@angular/cdk ^21` | ✅ |
| `@angular/cdk` | ^21.2 | — | ✅ (novo, exigido pelo PrimeNG) |
| `angular-oauth2-oidc` | 21.0.3 | `@angular/core >=21.0.0` | ✅ |
| `@primeuix/themes` | ^2.0.3 | — | ✅ |
| `primeflex` | ^4.0.0 | — | ✅ |
| `primeicons` | ^7.0.0 | — | ✅ |

> Angular 22 já é `latest`; 21 é `v21-lts`. O alvo é 21 conforme pedido — e é a versão que
> a referência usa, o que elimina divergência entre os dois projetos.

---

## 2. Ordem e princípio

**Upgrade antes de redesign.** Subir a versão sobre a base atual — pequena, sem biblioteca
de UI, com testes verdes — é muito mais barato do que subir versão sobre uma UI recém-escrita.
Se algo quebrar na Fase 2, a causa é o Angular, não o PrimeNG.

**Uma tela por vez, sempre navegável.** A partir da Fase 5 a aplicação nunca fica quebrada:
telas migradas e não migradas convivem (as antigas apenas feias). Nada de big bang.

**Verificação manual é obrigatória, não opcional.** O histórico deste projeto é explícito:
os bugs mais graves — precisão de TSID, `requireHttps` em build de produção, CORS em
`/.well-known/**`, issuer de dev — **não foram pegos por nenhum teste automatizado**, só por
smoke test real no navegador. Toda fase que toca autenticação termina com verificação
manual contra o backend rodando.

---

## Fase 0 — Baseline

- [ ] `npm install` e confirmar que `ng build` e `ng test` passam **hoje**, antes de tocar
      em qualquer coisa. Registrar tamanho do bundle inicial de produção.
- [ ] Confirmar smoke test manual do login + PKCE do console contra o backend
      (`mvn spring-boot:run` + `ng serve`), para ter certeza de que a base funciona.
- [ ] Commit de baseline — ponto de retorno seguro.

> Sem baseline verde, qualquer falha nas fases seguintes é ambígua.

---

## Fase 1 — Angular 19 → 20

- [ ] `ng update @angular/core@20 @angular/cli@20`
- [ ] `ng update angular-oauth2-oidc@20`
- [ ] Migração do builder: `@angular-devkit/build-angular` → `@angular/build`
      (schematic automático; `angular.json` passa a usar `@angular/build:application` e
      `@angular/build:dev-server`).
- [ ] Rodar as migrations automáticas de `inject()` e signals oferecidas pelo `ng update`.
- [ ] `ng build` + `ng test` verdes.

**Atenção:** manter `zone.js` nesta fase. Uma mudança de major por vez.

---

## Fase 2 — Angular 20 → 21 + zoneless

- [ ] `ng update @angular/core@21 @angular/cli@21`
- [ ] `ng update angular-oauth2-oidc@21`
- [ ] Remover `zone.js`: tirar de `polyfills` no `angular.json`, remover a dependência,
      trocar `provideZoneChangeDetection({ eventCoalescing: true })` por
      `provideBrowserGlobalErrorListeners()` (padrão da referência em `app.config.ts`).
- [ ] Remover `@angular/platform-browser-dynamic` (não existe mais em 21).
- [ ] Renomear componentes para o padrão sem sufixo (`LoginComponent` → `Login`), alinhando
      com a referência — opcional nesta fase, obrigatório até a Fase 5.
- [ ] `ng build` verde.

**Risco real — `angular-oauth2-oidc` sob zoneless.** A biblioteca usa timers internos
(renovação silenciosa de token, checagem de sessão). Sem Zone.js, atualizações disparadas
por esses timers podem não refletir na view. Mitigação: após esta fase, **smoke test manual
obrigatório** do fluxo PKCE completo (login → `/oauth2/authorize` → callback → dashboard),
não apenas `ng build`. Se houver problema, a correção é isolar a leitura de estado do
`OAuthService` em signals dentro de `ConsoleAuthService`, não reintroduzir Zone.js.

---

## Fase 3 — Migração do runner de testes para Vitest

- [ ] Remover Karma/Jasmine: `karma`, `karma-*`, `jasmine-core`, `@types/jasmine`.
- [ ] Adicionar `vitest` + `jsdom`; `angular.json` → `"test": { "builder": "@angular/build:unit-test" }`.
- [ ] Converter os 20 specs. Volume medido: **68 ocorrências de API do Jasmine em 9 arquivos**
      (`jasmine.createSpy` ×32, tipo `jasmine.Spy` ×35, `jasmine.objectContaining` ×1).
      Conversão mecânica: `jasmine.createSpy()` → `vi.fn()`, `jasmine.Spy` → `Mock`,
      `jasmine.objectContaining` → `expect.objectContaining`.
- [ ] Trocar `HttpClientTestingModule` (deprecado) por `provideHttpClientTesting()`.
- [ ] `ng test` verde com a mesma quantidade de testes de antes — nenhum teste some
      "acidentalmente" na migração.

> Fazer isto **antes** do redesign: os specs atuais são a única rede de proteção durante a
> reescrita das telas.

---

## Fase 4 — Fundação de design

- [ ] Instalar: `primeng@21`, `@angular/cdk@21`, `@primeuix/themes`, `primeflex`, `primeicons`.
- [ ] Dev: `prettier` (a referência usa; padroniza o diff das fases seguintes).
- [ ] `styles.css` → `styles.scss`; `angular.json` com
      `"styles": ["src/styles.scss", "node_modules/primeflex/primeflex.css"]` e
      `"inlineStyleLanguage": "scss"`, `schematics.@schematics/angular:component.style = "scss"`.
- [ ] Criar `src/assets/scss/**` conforme a seção 7.1 do guia de estilo (`base.scss`,
      `_variables.scss`, `_mixins.scss`, `_typography.scss`, `utils/`, `primeNG/`).
- [ ] Criar `core/config/providers/primeng.provider.ts` com o preset `AuthServerPreset`
      (código pronto na seção 2.4 do guia de estilo) e registrar em `app.config.ts`.
- [ ] Criar `i18n/primeng-pt.ts` (tradução do PrimeNG) e registrar `LOCALE_ID: 'pt-BR'` +
      `registerLocaleData(localePt)`.
- [ ] Providers globais: `MessageService`, `ConfirmationService`, `DialogService`.
- [ ] **Elevar os budgets** em `angular.json`: PrimeNG + primeflex estouram os atuais
      (500kB warn / 1MB error). Alvo da referência: 1MB / 1.5MB.
- [ ] Verificar tema claro e escuro com um componente PrimeNG qualquer antes de seguir.

---

## Fase 5 — Estrutura compartilhada

- [ ] `features/` → `pages/`, reorganizando cada módulo em
      `pages/<modulo>/{components, core/{models,dtos,services}}`.
- [ ] Portar da referência: `ListBase`, `FormBase`, `LayoutBasePages`, `FormLabel`,
      `LoadingOverlay`, wrapper de `MessageService`, `Toast`.
- [ ] Criar os componentes específicos deste projeto: **`StatusTag`** (mapeamento único de
      status → severidade, seção 2.3 do guia) e **`CopyField`** (valor mono + copiar +
      máscara, para `clientSecret`, IDs, redirect URIs).
- [ ] Criar os dois layouts:
      - `core/layout/public-layout` — card centralizado, branding do tenant, sem shell.
      - `core/layout/console-layout` — topbar + sidebar + `router-outlet`, com
        `LayoutService` (portado da referência) para responsividade do menu.
- [ ] Reestruturar `app.routes.ts` em duas árvores (pública e `/console`), com
      `loadComponent`/`loadChildren` — as rotas hoje são todas eager e planas.
- [ ] `ng build` + `ng test` verdes.

---

## Fase 6 — Telas públicas

Ordem deliberada: são as telas de maior impacto e menor risco (não dependem de token).

- [ ] `/login` — card, branding do tenant, `p-inputtext`/`p-password`, erro genérico
      preservado **literalmente** (seção 6.1 do guia).
- [ ] `/esqueci-senha` — resposta idêntica exista ou não o e-mail.
- [ ] `/reset-password` — `p-password` com `[feedback]` e regras de senha visíveis.
- [ ] `/consent` — lista de escopos, ações Autorizar/Negar.
- [ ] Estados de carregando/erro/sucesso desenhados em todas.
- [ ] **Smoke test manual** de cada tela contra o backend real (incluindo `/consent`, que
      exige um `System` com `thirdParty = true` — nunca foi testado manualmente, conforme
      nota no `PROGRESS.md` do backend).

---

## Fase 7 — Console

- [ ] Shell: topbar (marca, usuário, sair, alternador de tema) + sidebar (Tenants,
      Sistemas, Perfis, Usuários, Vínculos, Platform Admins).
- [ ] Dashboard — hoje é placeholder exibindo claims cruas. Substituir por visão útil
      (contagens, atalhos) **e corrigir o bug de sessão expirada** (guia, seção 6.5).
- [ ] Migrar CRUDs para `p-table` + `FormBase`, um por vez, cada um com paginação
      server-side, `StatusTag` e confirmação em ação destrutiva:
  - [ ] Tenants
  - [ ] Sistemas (incluindo redirect URIs e rotação de secret com `CopyField`)
  - [ ] Perfis
  - [ ] Usuários (incluindo reset de senha administrativo)
  - [ ] Vínculos (usuário → sistemas → perfis)
  - [ ] Platform Admins — **tela ainda não existe**; a admin-api já expõe o endpoint desde
        a Fase 8 do backend. Incluir aqui fecha a lacuna.
- [ ] Dark mode com persistência de preferência.

---

## Fase 8 — Fechamento

- [ ] `ng build --configuration production` dentro dos budgets.
- [ ] Smoke test com **nginx real** servindo o build de produção (reproduzir o roteiro
      registrado no `PROGRESS.md` do backend: `nginx:alpine` + `--add-host=auth-server:host-gateway`).
      Confirmar que `requireHttps: 'remoteOnly'` e `$http_host` continuam corretos.
- [ ] Roteiro E2E manual completo: login como platform admin → criar tenant → sistema →
      perfis → usuário → vincular → PKCE como usuário do tenant → validar claims do JWT.
- [ ] Revisar acessibilidade (contraste nos dois temas, foco, navegação por teclado).
- [ ] Atualizar `README.md` do frontend (hoje é boilerplate do Angular CLI, sem nada
      específico do projeto).
- [ ] Atualizar no backend: `docs/03-subir-ambiente-local.md` (versão do Node/Angular) e
      `PROGRESS.md` (nota registrando a modernização).

---

## 3. Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `angular-oauth2-oidc` com comportamento alterado sob zoneless | Média | Alto — quebra todo o console | Smoke test manual do PKCE ao fim da Fase 2, antes de qualquer redesign |
| Bundle estoura budget com PrimeNG | Alta | Baixo | Elevar budget na Fase 4 (já previsto); imports por módulo, nunca barrel |
| Regressão silenciosa em produção (não pega em `ng serve`) | Média | Alto | Fase 8 exige nginx real — precedente: dois bugs assim já ocorreram |
| Migração de specs escondendo perda de cobertura | Média | Médio | Comparar contagem de testes antes/depois na Fase 3 |
| Mensagem de erro deixar de ser genérica no redesign | Média | **Crítico** (enumeração de usuário) | Regra explícita no guia (6.1/6.2); revisar nos specs das telas públicas |
| ID virar `number` em algum model novo | Baixa | Alto | Regra no guia (6.3); bug com precedente real neste projeto |

---

## 4. Decisões tomadas

- **Alvo Angular 21, não 22.** Pedido do usuário, e é a versão da referência — mantém os
  dois projetos na mesma linha, sem divergência de ferramental.
- **Identidade visual própria, estrutura emprestada.** Herdamos de `questoes-concurso` a
  organização (SCSS, preset, base classes, convenção de pastas); a aparência é distinta por
  decisão explícita — ver seção 1.2 do guia de estilo.
- **Dois layouts, não um.** A referência tem um shell único porque é uma aplicação de uso
  contínuo. Aqui, as telas públicas e o console têm audiências e objetivos opostos; forçar
  o mesmo shell prejudicaria as duas.
- **PrimeNG também nas telas públicas.** Alternativa considerada: manter o login em HTML
  puro para minimizar o bundle da rota mais acessada. Descartado — as rotas públicas já são
  lazy-loaded e a inconsistência visual custaria mais do que os KB economizados.
- **Não migrar para Signal Forms.** Angular 21 traz a API nova, mas Reactive Forms é o que
  a referência usa e o que as base classes (`FormBase`) assumem. Adotar as duas coisas ao
  mesmo tempo dobraria a superfície de risco.

---

## 5. Em aberto

- **Onde entra o build do frontend no deploy** (Fase 11 do backend). Com os dois projetos
  em repositórios separados, o pipeline precisa buscar/buildar ambos — submodule, checkout
  duplo no GitHub Actions, ou `dist/` publicado como artefato. Já registrado no
  `PROGRESS.md` do backend; decidir ao iniciar a Fase 11.
- **Alternador de tema: persistir onde?** `localStorage` é o padrão, mas as telas públicas
  são de terceiros (usuários de tenants distintos no mesmo domínio). Provável decisão:
  console persiste em `localStorage`; telas públicas seguem `prefers-color-scheme` sem
  persistir.
