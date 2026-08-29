# Como o Auth Server V2 funciona

> Visão geral para quem está chegando agora no projeto. Para a especificação completa e
> as decisões de design, ver `# Plano de Implementação — Auth Server v2.md` na raiz do
> repo; para o histórico de implementação e notas técnicas, ver `PROGRESS.md`.

## 1. O que este sistema é (e o que não é)

É um **Authorization Server OAuth2/OIDC multi-tenant**. Ele centraliza a autenticação de
vários sistemas satélite (aplicações clientes) e responde a uma pergunta só:

> "Quem é este usuário, e quais perfis ele tem neste sistema?"

Ele **não é** um sistema de autorização. O token emitido carrega apenas **códigos de
perfil** (`["ADMIN", "OPERADOR"]`) — nunca permissões, telas ou ações. Cada sistema
satélite decide sozinho o que cada perfil pode fazer. Não existe (e não deve existir)
tabela de permissões, recursos ou ações neste projeto.

## 2. Modelo de domínio

```
tenant
  │
  └──< system_tenant >── system ──< system_profile
                            │             │
       user ──< user_system >             │
        │            │                    │
        │            └──< user_system_profile >
        │
   (user.tenant_id ─────────────> tenant)
```

| Conceito | O que é | Regra importante |
|---|---|---|
| **Tenant** | Organização cliente (ex: "Acme", "Globex") | `code` único global, fronteira de isolamento de dados |
| **System** | Aplicação satélite — é o *client OAuth2* | Pertence a exatamente 1 tenant; `clientId` único global |
| **SystemProfile** | Perfil dentro de um sistema (ex: `ADMIN`, `FINANCEIRO`) | Único por sistema (`UNIQUE (systemId, code)`), mas **repetível entre sistemas** — `ADMIN` pode existir em vários sistemas diferentes |
| **User** | Usuário final | Pertence a exatamente 1 tenant; `username`/`email` únicos **dentro do tenant** (o mesmo e-mail pode existir em tenants diferentes) |
| **PlatformAdmin** | O "usuário deus" | Tabela separada, opera acima de todos os tenants, nunca vinculado a um tenant |

Os vínculos (bindings) têm status próprio (`ACTIVE`/`INACTIVE`/`BLOCKED`):

- **SystemTenant** — liga um sistema ao seu único tenant.
- **UserSystem** — dá a um usuário acesso a um sistema. **Só pode existir se o usuário e
  o sistema pertencerem ao mesmo tenant** — essa é a regra de isolamento mais importante
  do projeto (ver seção 4).
- **UserSystemProfile** — dá a um `UserSystem` um perfil específico dentro daquele
  sistema.

## 3. O fluxo de login (Authorization Code + PKCE)

Não existe tela server-side (sem Thymeleaf). Todo o frontend — login, consentimento e
console administrativo — é um único projeto **Angular**, servido como SPA. O backend só
expõe API REST/JSON.

```
1. Satélite gera code_verifier + code_challenge (PKCE, S256)
2. Browser → GET /oauth2/authorize?client_id=CRM_ACME&redirect_uri=...&code_challenge=...
3. Auth server resolve o tenant a partir do client_id (via system_tenant)
4. Sem sessão válida → redireciona para a rota pública /login do Angular
5. Angular envia usuário/senha → POST /api/auth/login (endpoint de sessão, não OAuth2)
6. Login OK → cookie de sessão HttpOnly; Angular volta para GET /oauth2/authorize
7. Agora sucede → redirect para redirect_uri com ?code=...&state=...
8. Satélite → POST /oauth2/token (code + code_verifier) → recebe access_token + refresh_token
```

Ponto central: **o tenant vem sempre do `client_id`, nunca de input do usuário**. O
usuário não escolhe nem influencia seu tenant.

Duas identidades passam pelo mesmo `POST /api/auth/login`: usuário comum de tenant e
platform admin (usado pelo console administrativo). O `AuthenticationManager` tenta um
provider e cai para o outro conforme o `client_id` resolve ou não um `System`.

## 4. Isolamento entre tenants (prioridade #1)

Garantido em três camadas independentes — nenhuma confia sozinha na outra:

1. **Domínio** — `TenantConsistencyValidator` valida antes de qualquer criação de vínculo.
2. **Banco** — FKs compostas. `user_system.tenant_id` (redundante de propósito) participa
   de duas FKs compostas — `(user_id, tenant_id) → user(id, tenant_id)` e
   `(system_id, tenant_id) → system_tenant(system_id, tenant_id)` — apontando para a
   *mesma* coluna. Um `INSERT` cruzando tenants falha na FK mesmo via SQL direto.
3. **Repositório** — toda porta de saída (`*Repository`) recebe `TenantId` como primeiro
   parâmetro explícito. Um método de busca sem tenant é tratado como bug de segurança.

Um `Filter`/`ThreadLocal` de tenant global foi **deliberadamente rejeitado** (ver plano,
seção 8.2): falha silenciosamente em jobs assíncronos e listeners. O parâmetro explícito é
mais verboso, mas impossível de esquecer sem quebrar a compilação.

## 5. Cascata de status na autenticação

O acesso só é concedido se **todos** os níveis abaixo estiverem ativos, nesta ordem:

```
tenant → system → system_tenant → user → user_system → user_system_profile → system_profile
```

Qualquer um inativo/bloqueado interrompe a cadeia. Falhas de autenticação são sempre
genéricas (`"Invalid credentials"`) — a API nunca revela se o usuário existe, está
bloqueado, ou se foi a senha que errou.

## 6. O que vai dentro do token (claims)

```json
{
  "iss": "https://auth.seudominio.com",
  "sub": "532847592847592",
  "aud": "CRM_ACME",
  "exp": 1735689600,
  "tenant_id": "918273645",
  "tenant_code": "acme",
  "client_id": "CRM_ACME",
  "username": "joao.silva",
  "email": "joao@acme.com",
  "name": "João da Silva",
  "profiles": ["ADMIN", "FINANCEIRO"]
}
```

Token de **platform admin** (usado só para acessar `/admin/api/**` e o console) tem
formato diferente — sem `tenant_id`/`profiles`, com `"platform_admin": true`.

`profiles` contém só códigos. O mapeamento perfil → permissão é responsabilidade de cada
satélite (ver `docs/02-integracao-sistema-satelite.md`).

Assinatura **RS256**, chave pública exposta em `/oauth2/jwks` — os satélites validam a
assinatura sem precisar de segredo compartilhado. Access token: 60 min. Refresh token:
8h, com rotação a cada uso.

## 7. Estrutura do código (hexagonal)

```
domain/        regras de negócio puras — zero dependência de Spring/JPA
application/   casos de uso (port/in), portas de saída (port/out), serviços
adapter/in     controllers REST — admin (/admin/api/**) e auth público (/api/auth/**)
adapter/out    persistência JPA, envio de e-mail, geração de ID (TSID), segurança OAuth2
config/        SecurityConfig, AuthorizationServerConfig, CORS, rate limit, OpenAPI
```

Regras de dependência não negociáveis: `domain` não conhece nada de fora; nenhuma
entidade JPA cruza para `application`; nenhum DTO web entra em `application`. Ver seção 5
do plano para a lista completa (verificada por ArchUnit).

## 8. API administrativa — visão geral

Base `/admin/api/v1`, protegida por token de **platform admin**. Cobre CRUD de tenants,
sistemas, perfis, usuários, platform admins e os três tipos de vínculo. Documentação
OpenAPI/Swagger disponível em `/swagger-ui.html` (mesma exigência de auth). Detalhes
operacionais e exemplos de requisição estão em
`docs/04-guia-operacional-administracao.md`.

## 9. Estado atual do projeto

Fases 0–9 do plano completas (domínio, persistência, casos de uso, segurança/OAuth2, API
administrativa, frontend Angular). Fase 10 (qualidade — cobertura, ArchUnit completo,
seed de platform admin inicial) e Fase 11 (deploy AWS) ainda **não** foram feitas — ver
`PROGRESS.md` para o checklist detalhado e as notas de bugs encontrados/corrigidos.

Importante para quem for rodar localmente: **não existe seed automático do primeiro
platform admin.** Como criar o primeiro admin manualmente está descrito em
`docs/03-subir-ambiente-local.md`.
