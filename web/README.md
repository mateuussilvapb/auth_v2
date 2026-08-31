# Frontend — Console administrativo do Auth Server

Console de administração (tenants, sistemas OAuth2, perfis, usuários, vínculos, platform
admins) e telas públicas do fluxo PKCE (login, consentimento, recuperação de senha) do
`auth_v2`. Angular 21 zoneless, standalone components, signals, PrimeNG 21. Consome a
admin-api e o Authorization Server do backend em `../api`.

Ver `docs/GUIA-DE-ESTILO.md` (normativo, inclusive seção 8 de acessibilidade) e
`docs/PLANO-MODERNIZACAO.md`/`PROGRESS.md` para o histórico e as decisões de arquitetura
desta modernização (Angular 19→21, migração para Vitest, fundação de design com PrimeNG).

## Requisitos

- Node.js ≥ 20 (testado com 22.16 — sem `.nvmrc`/`engines` no `package.json` hoje).
- Backend (`../api`) rodando localmente (ver `../api/docs/03-subir-ambiente-local.md`) —
  o console não funciona sozinho, depende da admin-api e do Authorization Server.

## Desenvolvimento

```bash
npm install
npm start   # ng serve — http://localhost:4200, hot reload
```

Em desenvolvimento (`environment.development.ts`), `apiBaseUrl` aponta para
`http://localhost:8080` (origem separada do `:4200`) — o backend precisa liberar essa
origem em `authserver.cors.allowed-origins` (profile `dev`, já configurado em
`../api/src/main/resources/application-dev.yml`).

**Dev server ficando com estado obsoleto?** Se uma rota/tela nova não aparecer mesmo com
`npm run build` passando limpo, reinicie o processo do `ng serve` — o watcher do Vite já
travou nessa situação depois de rodar muito tempo com bastante atividade de arquivo
(achado registrado em `PROGRESS.md`, Fase 7).

## Build de produção

```bash
npm run build   # saída em dist/frontend, budgets em angular.json
```

Em produção (`environment.ts`), `apiBaseUrl` é `''` — o frontend espera ser servido na
**mesma origem** do backend, atrás de um reverse proxy (`nginx.conf` neste diretório
mapeia `/oauth2/`, `/.well-known/`, `/api/auth/`, `/admin/api/` e as rotas de swagger para
o backend; todo o resto cai no SPA com `try_files ... /index.html`). As variáveis
`AUTH_ISSUER` e `CONSOLE_REDIRECT_URIS` do backend precisam bater com a origem externa
real (não a porta interna) — ver `PROGRESS.md`, Fase 8, para o roteiro de smoke test com
nginx e os dois achados de configuração de ambiente.

## Testes

```bash
npm test   # Vitest (@angular/build:unit-test), zoneless — sem zone.js, sem Karma/Jasmine
```

Não há suíte de e2e automatizada — os roteiros de PKCE completo (login → `/oauth2/authorize`
→ `/oauth2/token` → claims do JWT) são validados manualmente contra o backend real,
documentados em `PROGRESS.md` (Fase 8, "Roteiro E2E manual completo").

## Estrutura

- `src/app/pages/` — telas, uma pasta por componente (`<nome>/<nome>.ts`, sem sufixo
  `Component`). Duas árvores: públicas (`login`, `consent`, `forgot-password`,
  `reset-password`) e `console/**` (protegidas por `consoleAuthGuard`).
- `src/app/core/` — serviços, guards, layouts e models compartilhados por várias páginas
  (`AdminApiService`, `AuthApiService`, `ConsoleAuthService`, `ThemeService`,
  `TenantContextService`), deliberadamente **não** fragmentados por página (guia de
  estilo, seção 1.1).
- `src/app/shared/` — componentes reutilizáveis sem estado de domínio (`FormBase`,
  `ListBase`, `FormLabel`, `StatusTag`, `CopyField`, `Toast`, `ConfirmDialog`).
- `src/assets/scss/` — fundação de design (tipografia, paleta, mixins de foco); preset
  PrimeNG em `src/app/core/config/providers/primeng.provider.ts`.
