---
name: modernize-auth-frontend-v2
description: Executa a modernização do frontend do Auth Server v2 (Angular 19 → 21 + zoneless, Vitest, PrimeNG 21) seguindo docs/PLANO-MODERNIZACAO.md e docs/GUIA-DE-ESTILO.md — trabalha item a item, valida cada passo com build e testes, corrige o que quebrar, e só commita item fechado com build e suíte verdes. Use quando o usuário pedir para continuar, avançar ou trabalhar na modernização do frontend (ex: "continua a modernização", "próxima fase do frontend", "migra as telas públicas").
---

# Modernize Auth Frontend V2

Executa o projeto descrito em `docs/PLANO-MODERNIZACAO.md` (ordem das fases, escopo de cada item,
riscos) e `docs/GUIA-DE-ESTILO.md` (normativo sobre cor, tipografia, componentes, estrutura de
pastas e regras de UX derivadas de segurança). Esses dois documentos são a fonte da verdade —
**releia a fase do plano e as seções do guia envolvidas antes de implementar cada item**, não
confie apenas na memória desta skill. Quando código e guia divergirem, o guia decide.

Esta skill não é "modernizar tudo de uma vez". Cada invocação avança um pedaço coerente de
trabalho (uma migração de versão, um componente compartilhado, uma tela) até fechar um item do
checklist, com build e testes verdes, e então commita. Sessões futuras retomam de onde pararam.

## Estado e retomada

Existe (ou crie na primeira execução) um arquivo `PROGRESS.md` na raiz do repo do frontend,
espelhando os checklists do plano, fase por fase. Antes de fazer qualquer coisa:

1. Leia `PROGRESS.md`. Se não existir, crie-o copiando os checklists do plano (Fase 0 a Fase 8),
   todos desmarcados, e com uma seção `## Notas` no fim para decisões e armadilhas encontradas.
2. Identifique a primeira fase com itens não marcados — essa é a fase corrente.
3. **Nunca pule para uma fase seguinte com itens pendentes na fase corrente.** O plano é explícito
   na seção 2: upgrade antes de redesign, uma major por vez, e cada fase verde antes da próxima.
4. Dentro da fase corrente, pegue o primeiro item não marcado como a unidade de trabalho desta
   rodada.

Se o usuário pedir explicitamente para pular a ordem (ex: "faz só a tela de login agora"), respeite
o pedido, mas registre em `PROGRESS.md` que a ordem padrão foi alterada por pedido explícito, e
avise se o item depende de fase anterior não concluída (ex: tela em PrimeNG antes da Fase 4 não tem
preset nem SCSS de base).

## Ciclo de trabalho por item

Para cada item do checklist fechado nesta rodada, siga exatamente esta sequência — não pule etapas
e não acumule vários itens não validados antes de rodar build e testes:

1. **Implementar** o item, seguindo à risca o guia de estilo: estrutura de pastas (7.1), base
   classes (7.2), convenções (7.3 — standalone, `inject()`, signals, kebab-case, classe sem sufixo
   `Component`, textos em pt-BR), padrões de componente (5) e acessibilidade (8).
2. **Escrever/atualizar testes** para o que foi implementado ou tocado — sem exceção. Vale para
   services, guards, componentes de tela, componentes compartilhados (`StatusTag`, `CopyField`) e
   base classes. Cobrir caminho feliz **e** os casos de erro. Para as telas públicas, o teste que
   garante que a mensagem de erro genérica não foi diferenciada (guia 6.1/6.2) é **obrigatório**,
   não opcional. Nas fases 1–3 (só migração de versão/runner), o requisito é que os testes
   existentes continuem existindo e passando, na **mesma quantidade** de antes.
3. **Validar o passo**, nesta ordem, sempre os dois:
   - `npm run build` (e, na Fase 8, também `ng build --configuration production` dentro dos
     budgets).
   - Suíte inteira, não só os testes novos: `ng test --watch=false` (até a Fase 2, Karma:
     `ng test --watch=false --browsers=ChromeHeadless`).
4. Se build ou teste falhar: **corrija até ficar verde**, nesta mesma rodada. Nunca commite com
   build quebrado, teste vermelho, `xit`/`it.skip`/`fdescribe`, `any` colocado para "calar o
   compilador", ou teste deletado para "resolver depois". Se a correção revelar uma armadilha não
   óbvia (ex: comportamento do `angular-oauth2-oidc` sob zoneless), registre-a nas `## Notas` do
   `PROGRESS.md`.
5. **Verificação manual quando o item toca autenticação ou aparência** — o plano (seção 2) é
   explícito: os bugs mais graves deste projeto não foram pegos por teste automatizado.
   Obrigatória ao fim das Fases 2, 6, 7 e 8, e sempre que o item alterar fluxo PKCE, login,
   consent ou reset de senha. Rode `ng serve` com o backend de pé (`mvn spring-boot:run` no
   `auth_api_v2`), execute o fluxo no navegador, e **peça ao usuário que confirme** quando não for
   possível verificar sozinho. Não marque o item como concluído com base só em `ng build`.
6. Só depois de tudo verde: **marque o item em `PROGRESS.md`** e faça o commit (formato abaixo).
7. Passe para o próximo item. Repita.

Regra dura: **um commit nunca mistura "implementação sem teste" com "teste depois, em outro
commit".** Implementação e os testes daquela funcionalidade entram juntos no mesmo commit, porque
o commit só acontece depois que build e suíte já passaram.

## Commits

- Um commit por item de checklist fechado (ou por grupo pequeno e coerente de itens estritamente
  relacionados, ex: os arquivos de `assets/scss/**` de uma vez — mas nunca uma fase inteira num
  commit só).
- Formato: Conventional Commits, em pt-BR, escopo pela área/fase:
  `chore(deps): atualiza Angular 19 para 20 e migra para @angular/build`,
  `test: migra specs de Jasmine para Vitest`,
  `feat(shared): adiciona StatusTag com mapeamento de status do backend`,
  `feat(login): reescreve tela de login com PrimeNG e branding do tenant`.
  Corpo de 1–2 linhas quando o "porquê" não for óbvio (ex: por que o budget subiu, por que uma
  mensagem de erro é genérica).
- Inclua a atualização de `PROGRESS.md` no mesmo commit da funcionalidade que ela fecha — não em
  commit separado.
- Nunca use `git commit --amend`, `--no-verify`, nem force-push.
- Se um hook de pre-commit falhar, corrija a causa raiz e crie um **novo** commit.
- Antes da primeira migração de versão (Fase 0), garanta o commit de baseline com build e testes
  verdes — é o ponto de retorno seguro de todas as fases seguintes.

## Regras não negociáveis (lembrete rápido — o guia é a fonte completa)

- **Todo ID é `string`, nunca `number`** (guia 6.3). TSID de 64 bits estoura
  `Number.MAX_SAFE_INTEGER` — bug com precedente real neste projeto. Vale para models, DTOs,
  rotas, bindings e comparações.
- **Não enumerar usuário** (guia 6.1/6.2). A UI exibe literalmente a mensagem genérica recebida do
  backend; é proibido diferenciar por texto, ícone, foco automático ou tempo de resposta.
  `/esqueci-senha` responde igual exista ou não o e-mail.
- **Branding de tenant não altera a UI da plataforma** (guia 6.4) — é conteúdo de terceiro,
  confinado ao cabeçalho do card público, `max-height: 48px`.
- **Sessão expirada é estado visível** (guia 6.5) — autenticação deriva de `expires_at`, nunca da
  mera presença do token.
- **`clientSecret` nunca em `localStorage`/`sessionStorage` nem em `console.log`** (guia 5.5).
- **Status por `StatusTag`, sempre com texto** (guia 2.3 e 8) — nenhum template decide cor de
  status por conta própria; `INACTIVE`/`DISABLED` são cinza, só `BLOCKED` é vermelho.
- **Toda ação destrutiva passa por `ConfirmationService`** com texto que nomeia o objeto e a
  consequência (guia 5.1). Nunca `alert()`/`confirm()` nativos.
- **Paginação server-side** em toda listagem (guia 5.3); três estados desenhados (carregando,
  vazio, erro).
- Reactive Forms — **não** migrar para Signal Forms (decisão registrada no plano, seção 4).
- Zoneless a partir da Fase 2: nada de `zone.js`, `setTimeout` para "forçar" detecção, nem
  `ChangeDetectorRef.detectChanges()` espalhado. Estado em signals.
- Sobrescrita de PrimeNG vai em `assets/scss/primeNG/_<componente>.scss`, nunca no SCSS da página,
  e sem `!important` nem gradiente.
- Preservar sem reescrita gratuita o que o plano marcou como já bom (1.1): `AdminApiService`,
  `AuthApiService`, `ConsoleAuthService`, `consoleAuthGuard`, `nginx.conf` (`$http_host`,
  `requireHttps: 'remoteOnly'`).

## Quando parar e perguntar

- Item do plano que exige verificação manual e você não consegue executá-la sozinho (login real,
  `/consent` com `System.thirdParty = true`, smoke test com nginx real) — peça a confirmação do
  usuário em vez de marcar o item.
- Os dois pontos em aberto do plano (seção 5): onde entra o build do frontend no deploy, e onde
  persistir a preferência de tema. Pergunte ao chegar no item correspondente.
- Decisão de produto/segurança não coberta pelo plano nem pelo guia. As decisões já tomadas
  (plano, seção 4; guia, seção 1.2) **não devem ser revisitadas** — não pergunte sobre elas.
- Comando destrutivo (reset de `node_modules` com mudanças não commitadas, force-push,
  `git checkout --` sobre trabalho não salvo) — sempre confirme antes.

Fora isso, prefira agir e reportar no fim da rodada o que foi fechado (itens marcados, commits
criados, resultado do build e da suíte, e o que ficou pendente de verificação manual), em vez de
pedir aprovação passo a passo.
