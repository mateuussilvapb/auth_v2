# Como integrar um sistema satélite

Um "sistema satélite" é qualquer aplicação (web, mobile, backend) que delega login a este
Auth Server em vez de implementar autenticação própria. Este guia cobre o que o
integrador precisa fazer dos dois lados: cadastro no Auth Server e implementação no
sistema satélite.

Pré-requisito de mentalidade: **este Auth Server só autentica, não autoriza.** Ele
responde "quem é" e "quais perfis tem". O que cada perfil pode fazer dentro do seu sistema
é decisão exclusivamente sua.

## 1. Cadastro do sistema no Auth Server

Feito pelo platform admin, via console administrativo Angular ou diretamente pela API
`/admin/api/v1` (ver `docs/04-guia-operacional-administracao.md` para o passo a passo
completo). Resumo:

1. O tenant dono do sistema já precisa existir (`POST /admin/api/v1/tenants`).
2. Criar o sistema sob esse tenant:

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

   - `clientId` é único globalmente e imutável — é o identificador OAuth2 que o satélite
     vai usar em toda chamada.
   - `publicClient: true` → SPA/mobile sem capacidade de guardar segredo (fluxo PKCE
     puro, `clientSecret` deve ser `null`). `publicClient: false` → backend confidencial
     que pode guardar segredo (informe `clientSecret`).
   - `initialRedirectUris` precisa conter toda URL de callback usada (dev, homolog,
     produção). URIs adicionais podem ser incluídas depois via
     `POST /admin/api/v1/systems/{id}/redirect-uris`.
   - `thirdParty: true` só se o sistema for de um parceiro externo e você quiser exigir
     tela de consentimento explícito antes de emitir o token. Sistemas próprios usam
     `false` (padrão) — login direto, sem tela de consentimento.

3. Criar os perfis que o sistema vai usar:

   ```
   POST /admin/api/v1/systems/{systemId}/profiles
   { "code": "ADMIN", "description": "Administrador do CRM" }
   ```

   Código único **dentro do sistema**; o mesmo código pode se repetir em outro sistema
   sem conflito.

4. Vincular os usuários que vão acessar esse sistema (`POST
   /admin/api/v1/tenants/{tenantId}/users/{userId}/systems` e depois vincular perfis) —
   ver guia operacional.

## 2. Fluxo OAuth2 que o satélite precisa implementar

Authorization Code + PKCE (S256), obrigatório — `code_challenge_method=plain` é
rejeitado.

```
1. Satélite gera code_verifier (string aleatória) e code_challenge = BASE64URL(SHA256(code_verifier))

2. Satélite redireciona o browser para:
   GET {issuer}/oauth2/authorize
     ?response_type=code
     &client_id=CRM_ACME
     &redirect_uri=https://crm.acme.com/callback
     &code_challenge={code_challenge}
     &code_challenge_method=S256
     &state={valor aleatório, para proteção CSRF — validar na volta}
     &scope=profile

3. Auth Server cuida de tudo (resolve tenant pelo client_id, mostra login Angular,
   autentica, opcionalmente pede consentimento) e redireciona de volta:
   GET https://crm.acme.com/callback?code={code}&state={mesmo state}

4. Satélite troca o code por tokens (chamada servidor-a-servidor):
   POST {issuer}/oauth2/token
   Content-Type: application/x-www-form-urlencoded

   grant_type=authorization_code
   &code={code}
   &redirect_uri=https://crm.acme.com/callback
   &client_id=CRM_ACME
   &code_verifier={code_verifier original}
   (+ autenticação do client se for confidencial — client_secret via Basic Auth ou body)

   → resposta:
   {
     "access_token": "eyJ...",
     "refresh_token": "...",
     "token_type": "Bearer",
     "expires_in": 3600
   }
```

Renovação de token (refresh token tem rotação a cada uso — sempre salve o novo):

```
POST {issuer}/oauth2/token
grant_type=refresh_token&refresh_token={refresh_token}&client_id=CRM_ACME
```

## 3. Validando o token no satélite

O access token é um **JWT assinado com RS256**. Não é preciso chamar o Auth Server para
validar — busque a chave pública uma vez (com cache) em:

```
GET {issuer}/oauth2/jwks
```

E valide localmente: assinatura, `exp` (expiração), `aud` (deve ser o seu `client_id`) e
`iss` (deve ser o issuer esperado). A maioria dos frameworks tem suporte nativo a
"resource server" JWT/JWKS (ex: Spring: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`).

Documento de descoberta OIDC padrão também está disponível:

```
GET {issuer}/.well-known/openid-configuration
```

## 4. Claims disponíveis no token de usuário

```json
{
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

**Regra de ouro:** o campo `profiles` é a única fonte de "quem este usuário é" que o
satélite deve usar para tomar decisões. **O mapeamento perfil → permissão/tela/ação é
responsabilidade exclusiva do satélite** — nunca peça ao Auth Server para carregar mais
que isso no token. Se você sentir necessidade de granularidade maior (permissões
específicas, recursos, ações), isso é modelado **dentro do seu sistema**, usando
`profiles` como entrada.

`sub` é o ID (TSID) do usuário — trate como string sempre; é um inteiro de 64 bits e pode
perder precisão se desserializado como número em JavaScript.

## 5. O que NÃO pedir ao Auth Server

- Endpoint de "o que este usuário pode fazer" — não existe e não vai existir (ver seção 1
  do plano de implementação).
- Scopes OAuth2 controlando acesso granular — o único scope usado é `profile`, fixo, só
  para o fluxo OAuth2 ter algo para negociar. Autorização real vem de `profiles` no token.
- Autenticação direta usuário/senha pelo satélite — a senha nunca deve passar pelo
  satélite; é por isso que existe a tela de login hospedada.

## 6. Checklist rápido de integração

- [ ] Tenant e sistema (`clientId`) cadastrados pelo platform admin
- [ ] Redirect URIs de todos os ambientes (dev/homolog/prod) cadastradas
- [ ] Perfis do sistema criados e usuários vinculados a eles
- [ ] Cliente OAuth2 no satélite implementa PKCE (S256) — nunca `plain`
- [ ] `state` gerado e validado no retorno (CSRF)
- [ ] Token validado localmente via JWKS (assinatura, `exp`, `aud`, `iss`)
- [ ] Refresh token: nova versão salva a cada rotação
- [ ] Lógica de perfil → permissão implementada dentro do satélite, nunca delegada de
      volta ao Auth Server
