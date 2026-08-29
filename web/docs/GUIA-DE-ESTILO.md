# Guia de Estilo — Auth Server Console

Definição da identidade visual e das regras de construção de interface do frontend do
Auth Server v2. Este documento é normativo: quando código e guia divergirem, o guia decide
(ou o guia é alterado deliberadamente, com registro do porquê).

Referência estrutural: `questoes-concurso` (mesma stack — PrimeNG 21, `@primeuix/themes`,
primeflex, SCSS em `assets/scss`). **A identidade visual é deliberadamente diferente** —
ver seção 1.2.

---

## 1. Identidade

### 1.1 O que este produto é

Um servidor de autenticação multi-tenant. Duas audiências, dois contextos de uso
completamente distintos:

| | Público (`/login`, `/consent`, `/esqueci-senha`, `/reset-password`) | Console (`/console/**`) |
|---|---|---|
| Quem usa | Usuário final do cliente (tenant) | Platform admin |
| Frequência | Segundos, uma vez por sessão | Horas, trabalho diário |
| Marca exibida | **Do tenant** (logo/nome via `/api/auth/branding`) | Da plataforma |
| Objetivo de design | Sair do caminho. Zero fricção, zero distração | Densidade de informação, previsibilidade |
| Shell | Nenhum. Card centralizado | Topbar + sidebar |

Essa divisão é a decisão de design mais importante do projeto. Tudo abaixo se organiza
em torno dela.

### 1.2 Divergência deliberada da referência

`questoes-concurso` tem identidade **gradiente quente** (vermelho → laranja, aplicado em
botões de ação primária via `.button-aplicacao-style`). Não reaproveitamos isso:

- **Cor**: paleta fria (índigo), não quente. Um console de identidade/segurança comunica
  confiança e sobriedade; vermelho é a cor que reservamos para operações destrutivas
  (bloquear usuário, desativar tenant) — gastá-la na identidade tira força do sinal.
- **Superfície**: cor **sólida**, não gradiente. Gradiente em botão primário é assinatura
  de marca do outro projeto; aqui o botão primário é chapado, o que também melhora o
  contraste previsível em dark mode.
- **Tipografia**: Manrope (não Inter), + JetBrains Mono para identificadores.
- **Raio**: 8px (não 4px) — linguagem mais suave, compensando a sobriedade da paleta.

Herdamos da referência a **estrutura** (organização SCSS, preset PrimeNG, base classes,
convenções de pasta), não a **aparência**.

---

## 2. Cor

### 2.1 Primária — Índigo

Ancorada em `500 = #6366f1`. Escala completa (a mesma que alimenta o preset PrimeNG):

| Token | Hex | Uso |
|---|---|---|
| `50` | `#eef2ff` | Fundo de destaque sutil, linha selecionada |
| `100` | `#e0e7ff` | Fundo de badge/tag informativa |
| `200` | `#c7d2fe` | Borda de elemento em foco suave |
| `300` | `#a5b4fc` | Texto de link em dark mode (hover) |
| `400` | `#818cf8` | **Primária em dark mode** |
| `500` | `#6366f1` | Âncora da identidade |
| `600` | `#4f46e5` | **Primária em light mode** (botão, link, ativo) |
| `700` | `#4338ca` | Hover de primária light |
| `800` | `#3730a3` | Active/pressed light |
| `900` | `#312e81` | Texto sobre fundo claro índigo |
| `950` | `#1e1b4b` | Contraste de texto sobre primária dark |

**Regra:** primária = ação e navegação. Nunca use índigo para comunicar estado de dado
(isso é papel dos status, 2.3).

### 2.2 Neutros

Escala slate (fria, harmoniza com índigo). Não usar cinzas puros (`#888`) — eles brigam
com a primária.

| Papel | Light | Dark |
|---|---|---|
| Fundo da aplicação | `#f8fafc` | `#0b1020` |
| Superfície (card, painel) | `#ffffff` | `#141a2e` |
| Superfície elevada (dialog, menu) | `#ffffff` + sombra | `#1b2238` |
| Borda / divisor | `#e2e8f0` | `#2a3350` |
| Texto primário | `#0f172a` | `#e8ecf6` |
| Texto secundário | `#475569` | `#9aa6c2` |
| Texto desabilitado | `#94a3b8` | `#5b678a` |

### 2.3 Semântica de estado

Os status vêm do domínio do backend e são fechados. Mapeamento **obrigatório** e único —
a mesma cor significa a mesma coisa em toda a aplicação:

| Status (backend) | Onde ocorre | Severidade PrimeNG | Cor | Rótulo pt-BR |
|---|---|---|---|---|
| `ACTIVE` | tenant, system, profile, user, binding, platform admin | `success` | verde | Ativo |
| `INACTIVE` | tenant, system, profile, binding, platform admin | `secondary` | neutro | Inativo |
| `BLOCKED` | user, binding | `danger` | vermelho | Bloqueado |
| `DISABLED` | user | `secondary` | neutro | Desabilitado |

Regras que decorrem disso:

- **`INACTIVE`/`DISABLED` não são erro.** São estado normal de ciclo de vida — cinza, não
  vermelho. Só `BLOCKED` (ação punitiva/segurança) é vermelho.
- **Bloqueio temporário por tentativas de login** (`User.lockedUntil`, backend Fase 7) não
  é um valor de `UserStatus`; é estado transitório. Exibir como badge `warn` (âmbar) com o
  horário de liberação, ao lado do badge de status — nunca sobrescrevendo o status.
- Cores semânticas (verde/vermelho/âmbar) **não** são temas de marca. Nunca as use para
  decoração.

### 2.4 Preset PrimeNG

Arquivo: `src/app/core/config/providers/primeng.provider.ts` (mesmo local da referência).

```ts
import Aura from '@primeuix/themes/aura';
import { definePreset } from '@primeuix/themes';
import { providePrimeNG } from 'primeng/config';

const AuthServerPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eef2ff', 100: '#e0e7ff', 200: '#c7d2fe', 300: '#a5b4fc',
      400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca',
      800: '#3730a3', 900: '#312e81', 950: '#1e1b4b',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.600}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}',
        },
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}',
        },
      },
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '{primary.950}',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        highlight: {
          background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
          focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
          color: '{primary.300}',
          focusColor: '{primary.200}',
        },
      },
    },
  },
});

export const PRIMENG_PROVIDER = providePrimeNG({
  theme: {
    preset: AuthServerPreset,
    options: { prefix: 'p', darkModeSelector: '.app-dark', cssLayer: false },
  },
  ripple: false,
  translation: primeNgTranslation, // i18n/primeng-pt.ts
});
```

Dark mode via classe `.app-dark` no `<html>` — mesmo mecanismo da referência.

---

## 3. Tipografia

```scss
$fontFamily: 'Manrope', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
$fontFamilyMono: 'JetBrains Mono', 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
```

**Mono não é decoração — é funcional.** Esta aplicação exibe muito identificador opaco onde
distinguir `0/O` e `1/l/I` importa e onde o usuário copia o valor literalmente. Use mono
obrigatoriamente em:

- `clientId` e `clientSecret` de sistema
- IDs (TSID) exibidos ou copiáveis
- Redirect URIs
- Claims de JWT no dashboard do console
- Códigos de tenant (`tenantCode`) e de perfil (`profileCode`)

Escala (base 16px, `$scale: 16px`):

| Papel | Tamanho | Peso | Uso |
|---|---|---|---|
| Display | 1.75rem | 700 | Título de página pública (login) |
| H1 | 1.5rem | 700 | Título de página do console |
| H2 | 1.25rem | 600 | Seção dentro da página |
| H3 | 1rem | 600 | Título de card |
| Body | 0.9375rem | 400 | Texto padrão |
| Small | 0.8125rem | 400 | Subtítulo, ajuda, metadados |
| Mono | 0.875rem | 500 | Identificadores |

Nunca usar peso < 400 (ilegível em telas comuns) nem > 700.

---

## 4. Tokens de forma, espaço e movimento

```scss
$borderRadius: 8px;      // padrão: card, input, botão
$borderRadiusSm: 6px;    // badge, tag, chip
$borderRadiusLg: 12px;   // dialog, card de login
$transitionDuration: 0.2s;
```

- **Espaçamento**: múltiplos de `0.25rem`, via classes primeflex (`gap-2`, `p-3`, `mb-4`).
  Não escrever `margin`/`padding` avulso em SCSS de componente quando existir utilitário.
- **Elevação**: apenas três níveis. Superfície plana (borda, sem sombra) → card
  (`0 1px 2px rgba(15,23,42,.06)`) → overlay (`0 8px 24px rgba(15,23,42,.12)`). Em dark
  mode, elevação é feita por **cor de superfície mais clara**, não por sombra.
- **Movimento**: `0.2s` para estado (hover, foco), `0.25s` para layout (abrir sidebar).
  Nada acima de `0.3s`. Respeitar `prefers-reduced-motion`.

---

## 5. Padrões de componente

### 5.1 Botões

| Variante | Quando | PrimeNG |
|---|---|---|
| Primário | Uma por tela, a ação principal (Salvar, Entrar) | `severity="primary"` |
| Secundário | Ações de apoio (Cancelar, Voltar) | `severity="secondary"` `[outlined]` |
| Texto | Ação terciária dentro de linha de tabela | `[text]` |
| Destrutivo | Bloquear, desativar, rotacionar secret | `severity="danger"` + confirmação |

**Toda ação destrutiva exige `ConfirmationService`** com texto que nomeia o objeto e a
consequência ("Desativar o tenant ACME? Usuários deste tenant não conseguirão autenticar.").
Nunca "Tem certeza?".

Sem gradiente. Sem `!important`. Se precisar sobrescrever PrimeNG, faça em
`assets/scss/primeNG/_<componente>.scss`, nunca no SCSS do componente de página.

### 5.2 Formulários

- Reactive Forms sempre. `FormBase` (ver 7.2) fornece `isInvalid()`, modos
  cadastro/edição/visualização e `pageId()`.
- Label sempre visível acima do campo (`FormLabel` compartilhado). Nunca placeholder como
  label.
- Erro **inline abaixo do campo**, só após `touched && dirty`. Mensagens em mapa
  `Record<string, string>` no componente (padrão `errorNomeMessages` da referência).
- Campo desabilitado em modo visualização, não escondido — o admin precisa ver o valor.
- Botão de submit desabilitado enquanto `submitting()`, com `[loading]`.

### 5.3 Listagens

`p-table` do PrimeNG com:

- Paginação **server-side** — a admin-api já pagina (`page`/`size`, `Page<T>`). Nunca
  carregar tudo e paginar no cliente.
- Coluna de status sempre como `p-tag` com o mapeamento de 2.3.
- Ações à direita, ícones com `pTooltip`.
- **Três estados obrigatórios**, todos desenhados: carregando (skeleton, não spinner
  solto), vazio (ilustração textual + ação primária: "Nenhum tenant cadastrado. Criar o
  primeiro"), erro (mensagem + botão "Tentar novamente").
- Busca client-side por `computed()` só quando a lista couber em uma página; caso
  contrário, filtro vai para a query.

### 5.4 Feedback

- **Toast** (`MessageService` wrapper, 7.2) para resultado de ação: criou, atualizou,
  falhou. `life: 5000`.
- **Inline** para erro de validação de campo.
- **Dialog de confirmação** para destrutivo.
- Nunca `alert()`/`confirm()` nativos — quebram o fluxo e não são estilizáveis.

### 5.5 Exibição de segredo

`clientSecret` só é retornado na criação e na rotação. O componente deve:

1. Exibir em mono, dentro de um bloco com fundo de destaque.
2. Oferecer botão "Copiar".
3. Exibir aviso explícito: "Este é o único momento em que o secret é exibido. Guarde-o
   agora."
4. **Nunca** persistir em `localStorage`/`sessionStorage` nem logar em console.

---

## 6. Regras de UX derivadas de segurança

Estas não são preferências estéticas — são requisitos do backend que a UI **não pode
violar**. Estão aqui porque são fáceis de quebrar "melhorando a experiência".

1. **Não enumerar usuário.** O backend retorna erro genérico e idêntico para
   usuário inexistente, senha errada, tenant inativo, usuário bloqueado e `client_id`
   desconhecido (Fases 5/7). A UI exibe exatamente a mensagem recebida. É **proibido**
   diferenciar ("usuário não encontrado", "senha incorreta"), inclusive por sinal indireto:
   tempo de resposta simulado, foco automático em campo específico, ou ícone diferente.
2. **`/esqueci-senha` sempre responde igual**, exista ou não o e-mail. A tela de sucesso é
   a mesma nos dois casos: "Se o e-mail estiver cadastrado, enviaremos as instruções."
3. **Todo ID é `string`, nunca `number`.** TSIDs de 64 bits estouram
   `Number.MAX_SAFE_INTEGER` — bug real já ocorrido em produção neste projeto (ver
   `PROGRESS.md` do backend). Vale para models, rotas, `[(ngModel)]` e comparações.
4. **Branding de tenant não altera a UI da plataforma.** Logo e nome do tenant vêm de
   endpoint público e são conteúdo controlado por terceiro. Podem ocupar **apenas** o
   cabeçalho do card público, com `max-height` fixo (48px) e sem afetar cor, layout ou
   qualquer superfície do console. Tenant sem logo → exibir o nome em texto, nunca um
   placeholder quebrado.
5. **Sessão expirada é estado visível.** O dashboard não pode exibir "Logado como X" com
   token expirado (bug de UX registrado no `PROGRESS.md` do backend, ainda aberto). O
   estado de autenticação deriva de `expires_at`, não da mera presença do token.

---

## 7. Organização do código

### 7.1 Estrutura de pastas

Espelha a referência, adaptada às duas zonas:

```
src/
├── assets/scss/
│   ├── base.scss                 # agregador (@use de tudo)
│   ├── base/_reset.scss
│   ├── _typography.scss
│   ├── utils/_variables.scss     # tokens (cor, raio, fonte, transição)
│   ├── utils/_mixins.scss        # focused(), focused-inset()
│   ├── utils/_topbar.scss
│   ├── utils/_menu.scss
│   ├── utils/_content.scss       # layout-main-container, layout-main
│   └── primeNG/_*.scss           # overrides de componente PrimeNG
├── app/
│   ├── core/                     # infraestrutura, uma instância por app
│   │   ├── components/{topbar,sidebar}/
│   │   ├── config/providers/primeng.provider.ts
│   │   ├── guards/
│   │   ├── layout/
│   │   │   ├── console-layout/   # topbar + sidebar + router-outlet
│   │   │   └── public-layout/    # card centralizado, branding do tenant
│   │   ├── models/
│   │   └── services/
│   ├── i18n/primeng-pt.ts
│   ├── shared/                   # reutilizável, sem regra de negócio
│   │   ├── components/{form-base,list-base,layout-base-pages,form-label,
│   │   │                status-tag,copy-field,loading-overlay,toast}/
│   │   └── services/{message.service,loading-overlay.service}.ts
│   └── pages/
│       ├── login/ | consent/ | forgot-password/ | reset-password/
│       └── console/
│           ├── dashboard/
│           ├── tenants/{components/, core/{models,dtos,services}}
│           ├── systems/ | profiles/ | users/ | bindings/
```

Regra da referência mantida: cada página é `pages/<modulo>/` com `components/` (telas e
apresentação) e `core/` (`models`, `dtos`, `services`, `<modulo>.routes.ts`).

### 7.2 Base classes compartilhadas

Portadas da referência, sem alteração de contrato:

- **`ListBase`** (`@Directive({})`): injeta `router`, `fb`, `dialogService`,
  `messageService`, `confirmationService`; expõe `form` e `submitting`.
- **`FormBase extends ListBase`**: modos (`isCreateMode`/`isEditMode`/`isViewMode`),
  `pageId()`, `isInvalid(control)`, `finalizar()` (fecha dialog ou navega).
- **`LayoutBasePages`**: cabeçalho de página (título, subtítulo, botão de ação) +
  `<ng-content>`.
- **`MessageService`** (wrapper do PrimeNG): `showSuccess/showError/showWarning/showInfo`.

Adições específicas deste projeto:

- **`StatusTag`**: recebe o status cru do backend, aplica o mapeamento de 2.3. Centraliza
  a regra — nenhum template decide cor de status por conta própria.
- **`CopyField`**: valor em mono + botão copiar + máscara opcional (secret, ID, URI).

### 7.3 Convenções

- Componentes standalone, `imports` agrupados por bloco comentado (`//Angular`,
  `//Aplicação`, `//Externos`) — padrão da referência.
- Estado com **signals** (`signal`, `computed`); nada de `BehaviorSubject` para estado de
  componente.
- Injeção por `inject()`, não constructor.
- Nomes de arquivo em kebab-case; classe em PascalCase sem sufixo `Component` (padrão
  Angular 21 / referência: `BancasListPage`, não `BancasListPageComponent`).
- SCSS de componente só para o que é genuinamente local. Layout → primeflex.
- Textos de interface em **pt-BR**. Identificadores técnicos (`clientId`, `ACTIVE`)
  permanecem como estão — não traduzir valor, só o rótulo exibido.

---

## 8. Acessibilidade

Mínimo não negociável:

- Contraste AA (4.5:1 texto normal, 3:1 texto grande) — verificado nos dois temas. O par
  `primary.600` sobre branco e `primary.400` sobre `#0b1020` foi escolhido por atender isso.
- Foco sempre visível, via mixin `focused()` (`box-shadow: var(--p-focus-ring)`). Nunca
  `outline: none` sem substituto.
- Todo campo com `<label for>`; erro associado por `aria-describedby`; `aria-invalid`.
- Toast de erro com `role="alert"`.
- Navegação completa por teclado, inclusive tabela e dialog.
- Ícone sozinho (ação em tabela) sempre com `aria-label`.
- Status nunca comunicado só por cor — o `p-tag` sempre traz o texto do estado.
