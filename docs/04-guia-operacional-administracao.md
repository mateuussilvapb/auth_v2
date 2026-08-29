# Guia operacional — administração via API

Passo a passo prático das operações administrativas mais comuns: criar o usuário deus,
tenants, sistemas, perfis, usuários e vincular tudo. Toda a API abaixo fica sob
`/admin/api/v1` e **exige um token Bearer de platform admin** (exceto a criação do
primeiro platform admin, que é feita manualmente — ver `docs/03-subir-ambiente-local.md`,
seção 4).

Tudo aqui também pode ser feito pelo console administrativo Angular (`/console`), que
consome exatamente esses mesmos endpoints. Este guia foca na API, útil para automação e
para entender o que o console faz por baixo.

> IDs de todos os recursos são **TSID** (inteiros de 64 bits) — sempre trate como string
> ao trafegar em JSON/JavaScript; um número JS pode perder precisão nesse tamanho.

## 0. Autenticando como platform admin

```
POST /api/auth/login
{ "usernameOrEmail": "admin", "password": "TrocarEssaSenha123" }
```

Isso autentica por sessão (usado pelo console Angular via cookie). Para chamar a API
`/admin/api/v1` diretamente com um Bearer token, faça o fluxo OAuth2/PKCE completo pelo
client `console` (ver `docs/02-integracao-sistema-satelite.md` para o mecanismo do
fluxo) e use o `access_token` retornado:

```
Authorization: Bearer eyJ...
```

Documentação interativa (Swagger, protegida da mesma forma): `/swagger-ui.html`.

## 1. Usuário deus (Platform Admin)

Depois de existir o primeiro (seed manual), os demais podem ser criados normalmente:

```
POST /admin/api/v1/platform-admins
{ "username": "maria.souza", "email": "maria@empresa.com", "password": "SenhaForte123", "name": "Maria Souza" }
```

Listar: `GET /admin/api/v1/platform-admins` (paginado).

Ativar/desativar:

```
PATCH /admin/api/v1/platform-admins/{id}/status
{ "status": "ACTIVE" }   -- ou "INACTIVE"
```

Regra de negócio protegida (`PlatformAdminPolicy`): **nunca é possível desativar o
último platform admin ativo** — a API rejeita a tentativa.

## 2. Tenant

```
POST /admin/api/v1/tenants
{ "code": "acme", "name": "Acme Ltda" }
```

`code` é único global e **imutável** após criado (usado em URLs/branding, não em
lógica de negócio — quem identifica o tenant no fluxo de login é sempre o `client_id` do
sistema, nunca o código do tenant).

```
GET  /admin/api/v1/tenants                 -- lista paginada
GET  /admin/api/v1/tenants/{id}            -- detalhe
PUT  /admin/api/v1/tenants/{id}
     { "name": "Acme S.A." }               -- só o nome é editável
PATCH /admin/api/v1/tenants/{id}/status
     { "status": "INACTIVE" }              -- desativa TODOS os sistemas/logins desse tenant
```

Tenant `INACTIVE` bloqueia login em **todos** os sistemas vinculados a ele (cascata de
status — ver `docs/01-como-funciona.md`).

## 3. Sistema (satélite / client OAuth2)

Sempre criado sob um tenant específico:

```
POST /admin/api/v1/tenants/{tenantId}/systems
{
  "clientId": "CRM_ACME",
  "name": "CRM da Acme",
  "publicClient": true,
  "clientSecret": null,
  "initialRedirectUris": ["https://crm.acme.com/callback"],
  "thirdParty": false
}
```

- `publicClient: true` (SPA/mobile, sem segredo) vs. `false` (backend confidencial, exige
  `clientSecret`).
- `thirdParty: true` só para parceiros externos — ativa a tela de consentimento OAuth2
  antes de emitir token. Não dá para mudar depois de criado (sem endpoint dedicado).

```
GET  /admin/api/v1/tenants/{tenantId}/systems       -- lista os sistemas do tenant
PUT  /admin/api/v1/systems/{id}
     { "name": "CRM da Acme v2" }                    -- só o nome
PATCH /admin/api/v1/systems/{id}/status
     { "status": "INACTIVE" }

POST   /admin/api/v1/systems/{id}/redirect-uris
       { "uri": "https://crm.acme.com/callback-staging" }
DELETE /admin/api/v1/systems/{id}/redirect-uris?uri=https://crm.acme.com/callback-staging

POST /admin/api/v1/systems/{id}/rotate-secret
     { "newSecret": "novo-segredo-forte" }           -- só faz sentido para clients confidenciais
```

## 4. Perfil (SystemProfile)

Sempre criado sob um sistema específico. Código único **dentro do sistema** — pode
repetir em outro sistema sem problema (ex: `ADMIN` em vários sistemas diferentes):

```
POST /admin/api/v1/systems/{systemId}/profiles
{ "code": "ADMIN", "description": "Administrador do CRM" }
```

```
GET  /admin/api/v1/systems/{systemId}/profiles
GET  /admin/api/v1/systems/{systemId}/profiles/{id}
PUT  /admin/api/v1/systems/{systemId}/profiles/{id}
     { "description": "Administrador com acesso total" }   -- só a descrição é editável, código é imutável
PATCH /admin/api/v1/systems/{systemId}/profiles/{id}/status
     { "status": "INACTIVE" }
```

## 5. Usuário

Sempre criado sob um tenant específico. `username`/`email` únicos **dentro do tenant**
(o mesmo e-mail pode existir em tenants diferentes sem conflito):

```
POST /admin/api/v1/tenants/{tenantId}/users
{ "username": "joao.silva", "email": "joao@acme.com", "password": "SenhaForte123", "name": "João da Silva" }
```

Isso dispara e-mail de boas-vindas (`EmailSenderPort` — em dev, capturado pelo MailHog em
`http://localhost:8025`).

```
GET  /admin/api/v1/tenants/{tenantId}/users
GET  /admin/api/v1/tenants/{tenantId}/users/{id}
PUT  /admin/api/v1/tenants/{tenantId}/users/{id}
     { "name": "João da Silva Jr.", "email": "joao2@acme.com" }
PATCH /admin/api/v1/tenants/{tenantId}/users/{id}/status
     { "status": "ACTIVE" }     -- ACTIVE | BLOCKED | DISABLED

POST /admin/api/v1/tenants/{tenantId}/users/{id}/reset-password
     -- dispara e-mail de redefinição (fluxo "esqueci minha senha" administrativo, sem corpo)
```

## 6. Vincular usuário a sistema e a perfis

Duas etapas: primeiro vincula usuário↔sistema, depois vincula perfis dentro desse
vínculo.

### 6.1 Vincular usuário a um sistema

```
POST /admin/api/v1/tenants/{tenantId}/users/{userId}/systems
{ "systemId": "123456789012345" }
```

**O sistema precisa pertencer ao mesmo tenant do usuário** — tentar vincular a um
sistema de outro tenant falha com 400 (`TenantConsistencyValidator`), tanto pela API
quanto se alguém tentasse via SQL direto (FK composta no banco).

Listar vínculos do usuário: `GET
/admin/api/v1/tenants/{tenantId}/users/{userId}/systems` (usado pelo console para montar
a tela de vínculos).

Mudar status do vínculo:

```
PATCH /admin/api/v1/tenants/{tenantId}/user-systems/{id}/status
{ "status": "ACTIVE" }   -- ACTIVE | INACTIVE | BLOCKED
```

### 6.2 Vincular um perfil dentro desse vínculo

```
GET  /admin/api/v1/tenants/{tenantId}/user-systems/{userSystemId}/profiles

POST /admin/api/v1/tenants/{tenantId}/user-systems/{userSystemId}/profiles
{ "profileId": "223456789012345" }   -- precisa ser um SystemProfile do MESMO sistema do vínculo

PATCH /admin/api/v1/tenants/{tenantId}/user-systems/{userSystemId}/profiles/{id}/status
{ "status": "BLOCKED" }
```

Só perfis do sistema ao qual o `UserSystem` está vinculado podem ser adicionados — perfil
de outro sistema é rejeitado.

## 7. Roteiro completo de ponta a ponta

Sequência mínima para deixar um usuário logando de verdade num sistema novo:

```
1. POST /admin/api/v1/tenants                                       → cria tenant "acme"
2. POST /admin/api/v1/tenants/{tenantId}/systems                    → cria sistema "CRM_ACME"
3. POST /admin/api/v1/systems/{systemId}/profiles                   → cria perfil "ADMIN"
4. POST /admin/api/v1/tenants/{tenantId}/users                      → cria usuário "joao.silva"
5. POST /admin/api/v1/tenants/{tenantId}/users/{userId}/systems     → vincula usuário ao sistema
6. POST /admin/api/v1/tenants/{tenantId}/user-systems/{id}/profiles → vincula perfil ADMIN
7. Usuário faz login via PKCE no client_id "CRM_ACME" → token com tenant_id, client_id
   e profiles: ["ADMIN"]
```

Este é exatamente o roteiro de verificação manual da seção 14 do plano de implementação,
já validado ponta a ponta (ver `PROGRESS.md`).

## 8. Erros comuns e o que significam

| Situação | Resultado |
|---|---|
| E-mail/username já existe no mesmo tenant | 422, `DomainException` |
| Vincular usuário a sistema de outro tenant | 400, mensagem clara de inconsistência de tenant |
| Vincular perfil de outro sistema ao `UserSystem` | 400 |
| Perfil duplicado no mesmo sistema | 422 (mas o mesmo código em outro sistema funciona normalmente) |
| Desativar o último platform admin ativo | 422, rejeitado pela `PlatformAdminPolicy` |
| Login com tenant/sistema/vínculo/perfil inativo em qualquer nível da cascata | 401 genérico, `"Invalid credentials"` — nunca revela em qual nível falhou |
