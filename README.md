# Auth Server V2

Authorization Server OAuth2/OIDC **multi-tenant**, responsável apenas por autenticação
(*"quem é este usuário, e quais perfis ele tem neste sistema?"*). Centraliza login para
os sistemas satélite (ex: [`sistema_promissorias`](../../sistema_promissorias)) — eles não
implementam login próprio, não guardam usuários, não guardam senhas.

Este repositório reúne backend e frontend, antes mantidos em repositórios separados
(`auth_api_v2` e `auth_frontend_v2`); o histórico de commits de ambos foi preservado
(`git log api/` e `git log web/` continuam mostrando o histórico original de cada um).

---

## Stack

| Componente | Pasta | Porta | Tecnologia |
|---|---|---|---|
| Backend | [`api/`](api) | 8080 | Java 25 · Spring Boot 4.x · Maven · arquitetura hexagonal |
| Frontend | [`web/`](web) | 4200 | Angular 21 · PrimeNG 21 |
| Banco | — | 5432 | PostgreSQL · Flyway |
| E-mail (dev) | — | 8025 | MailHog |

---

## Documentação

Cada lado mantém sua própria documentação interna (não duplicada aqui):

| # | Documento | O quê |
|---|---|---|
| 1 | [`api/docs/03-subir-ambiente-local.md`](api/docs/03-subir-ambiente-local.md) | **Comece aqui para rodar** — backend + banco + e-mail, e depois o frontend |
| 2 | [`api/# Plano de Implementação — Auth Server v2.md`](<api/# Plano de Implementação — Auth Server v2.md>) | Especificação completa do backend — arquitetura, decisões, modelo de dados, fases |
| 3 | [`api/docs/01-como-funciona.md`](api/docs/01-como-funciona.md) | Visão geral do Auth Server para quem chega agora |
| 4 | [`api/docs/02-integracao-sistema-satelite.md`](api/docs/02-integracao-sistema-satelite.md) | Como integrar um sistema satélite (ex: promissórias) |
| 5 | [`api/docs/04-guia-operacional-administracao.md`](api/docs/04-guia-operacional-administracao.md) | Operações administrativas via API |
| 6 | [`web/docs/PLANO-MODERNIZACAO.md`](web/docs/PLANO-MODERNIZACAO.md) | Plano de modernização do frontend (Angular 19 → 21 + PrimeNG) |
| 7 | [`web/docs/GUIA-DE-ESTILO.md`](web/docs/GUIA-DE-ESTILO.md) | Guia de estilo visual do console — normativo |
| 8 | [`api/PROGRESS.md`](api/PROGRESS.md) | Progresso do backend, fase a fase |
| 9 | [`web/PROGRESS.md`](web/PROGRESS.md) | Progresso do frontend, fase a fase |

## Ambiente local (resumo)

```bash
# 1. infra (Postgres + MailHog) — a partir de api/
cd api && docker compose up -d

# 2. backend
export DB_USERNAME=authserver DB_PASSWORD=authserver EMAIL_SENDER=dev@localhost
mvn spring-boot:run

# 3. frontend, em outro terminal
cd ../web
npm install
ng serve
```

Passo a passo completo, com armadilhas conhecidas, em
[`api/docs/03-subir-ambiente-local.md`](api/docs/03-subir-ambiente-local.md).

## Estrutura

```
auth_v2/
├── api/    — backend (Java/Spring), histórico de auth_api_v2
└── web/    — frontend (Angular), histórico de auth_frontend_v2
```
