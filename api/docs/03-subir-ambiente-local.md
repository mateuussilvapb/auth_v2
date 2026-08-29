# Como subir o ambiente local

Passo a passo completo, do zero, para rodar o backend + banco + e-mail local. Para depois
subir o frontend Angular junto, ver a seção 6.

> **Frontend em `../web`.** O frontend Angular vive em [`web/`](../../web), pasta irmã de
> `api/` dentro do mesmo repositório `auth_v2`. A seção 6 assume esse layout.

## Pré-requisitos

- Java 25 e Maven instalados
- Docker (para Postgres + MailHog via `docker-compose.yml`)
- Node.js + npm (só se for rodar o frontend Angular também — pasta `web/`)

## 1. Subir a infraestrutura (Postgres + MailHog)

Na raiz do repo:

```bash
docker compose up -d
```

Isso sobe:

- **Postgres** em `localhost:5432`, banco/usuário/senha `authserver`/`authserver`/`authserver`
  (uso local apenas — nunca use essas credenciais em produção).
- **MailHog** — captura os e-mails que a aplicação envia (boas-vindas, reset de senha) sem
  enviar de verdade. UI web em **http://localhost:8025**.

Confirme que os containers subiram:

```bash
docker compose ps
```

## 2. Definir variáveis de ambiente

O projeto não versiona nenhuma credencial (`application.yml` só referencia `${VAR}`). O
profile `dev` (`application-dev.yml`) já tem defaults para tudo que combina com o
`docker-compose.yml` acima — na prática, **para rodar local só é obrigatório** definir:

```bash
export EMAIL_SENDER=dev@localhost
```

As demais (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SMTP_*`, `AUTH_ISSUER`) já têm default
correto em `application-dev.yml` para bater com o Postgres/MailHog do compose. Só
precisam ser exportadas se você mudar algo do compose ou quiser sobrescrever.

Alternativa mais cômoda: criar `src/main/resources/application-local.yml` (já está no
`.gitignore`, nunca vai para o repo) com os overrides que quiser, e ativar o profile
`local` junto do `dev` (seção 5 abaixo).

## 3. Rodar as migrations e subir a aplicação

O Flyway roda automaticamente no boot (V1–V14 hoje). Basta subir a aplicação:

```bash
mvn spring-boot:run
```

Por padrão o profile ativo é `dev` (definido em `application.yml`). A API sobe em
**http://localhost:8080**.

Verifique a saúde:

```bash
curl http://localhost:8080/actuator/health
```

> **Atenção (dev vs. produção):** hoje `/actuator/**` não passa por nenhum filter chain de
> segurança — fica exposto sem autenticação. Isso é aceitável em dev, mas é um item
> pendente antes de qualquer deploy real (ver `PROGRESS.md`, Fase 11).

## 4. Criar o primeiro Platform Admin (usuário deus)

**Importante:** o projeto ainda não tem seed automático do primeiro platform admin (item
em aberto na Fase 10 do `PROGRESS.md`). Todos os endpoints de
`/admin/api/v1/platform-admins`, incluindo o de criação, **exigem token de platform
admin** — ou seja, não dá para criar o primeiro pela API. É preciso inserir direto no
banco, uma única vez.

### 4.1 Gerar o hash BCrypt da senha

A senha é armazenada com BCrypt custo 12 (`Password`, VO do domínio). Gere o hash usando
o classpath do próprio projeto — não precisa de nenhuma ferramenta externa:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q
jshell --class-path "target/classes;$(cat cp.txt)"
```

Dentro do `jshell`:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
new BCryptPasswordEncoder(12).encode("TrocarEssaSenha123");
```

Copie o hash impresso (algo como `$2a$12$...`) e saia com `/exit`.

### 4.2 Inserir o registro

Conecte no Postgres do compose (`docker exec -it <container_postgres> psql -U authserver
-d authserver`, ou qualquer client SQL) e rode:

```sql
INSERT INTO platform_admin (id, username, email, password_hash, name, status, created_at, created_by)
VALUES (
    1,                              -- qualquer BIGINT único; não precisa ser TSID de verdade
    'admin',
    'admin@promissorias.local',     -- precisa de TLD com ponto — "admin@localhost" quebra
                                     -- Email.of() ao reconstruir o agregado do banco (o VO
                                     -- exige `\.[A-Za-z]{2,}` após o @), e o erro só aparece
                                     -- no login, como "Formato de email inválido" (400)
    '$2a$12$...',                  -- hash gerado no passo 4.1
    'Administrador',
    'ACTIVE',
    NOW(),
    'seed-manual'
);
```

Pronto — agora dá para logar com `admin` / `TrocarEssaSenha123` e usar esse token para
criar os próximos platform admins pela API normalmente (`POST
/admin/api/v1/platform-admins`), se precisar de mais de um.

## 5. Rodar os testes

```bash
mvn test      # unitários
mvn verify    # unitários + integração (Testcontainers) + ArchUnit
```

`mvn verify` sobe um Postgres via Testcontainers automaticamente — não depende do
`docker-compose.yml` estar no ar.

## 6. Subir o frontend Angular (opcional, para testar o fluxo completo)

Pasta irmã dentro do mesmo repo:

```bash
cd ../web
npm install
ng serve
```

Frontend sobe em **http://localhost:4200**. Como backend (`:8080`) e frontend (`:4200`)
rodam em origens diferentes em dev, o profile `dev` já cobre isso (CORS liberado para
`localhost:4200`, issuer OIDC apontando para `localhost:8080`).

Login do console administrativo: acesse `http://localhost:4200/console`, será redirecionado
para o fluxo OAuth2/PKCE e cai na tela de login — use o platform admin criado no passo 4.

Para testar o fluxo de login como **usuário comum** de um sistema satélite específico, é
preciso primeiro cadastrar tenant/sistema/perfis/usuário — ver
`docs/04-guia-operacional-administracao.md`.

## 7. Seed de dados de desenvolvimento (script)

Repetir os passos 3-6 manualmente (platform admin, tenant, system, 4 perfis, usuário de
teste) toda vez que o volume do Postgres for resetado é lento e propenso a erro — foi
exatamente isso que causou o incidente registrado no `PROGRESS.md` do `sistema_promissorias`
(gate S094, 2026-08-21): o ambiente local precisou ser reconstruído do zero para validar
login/navegação, e refazer isso à mão a cada sessão não escala.

`scripts/dev-seed.js` automatiza os passos 4-6 inteiros (platform admin via SQL direto +
login/PKCE do console + tenant + system `PROMISSORIAS` + 4 perfis + usuário de teste), via
chamadas HTTP diretas à Admin API. **Não é commitado** (está no `.gitignore` — é dado de
desenvolvimento local, não código do projeto). Se o arquivo não existir na sua cópia,
peça para ele ser gerado novamente a partir desta seção + do contrato da Admin API em
`src/main/java/.../adapter/in/web/admin/`.

Pré-requisitos: Postgres do compose no ar (`docker compose up -d`), Auth Server rodando em
`:8080` (passo 3), Node.js disponível (só usa módulos nativos — sem `npm install`).

```bash
node scripts/dev-seed.js
```

**Não é idempotente** — assume volume vazio/recém-resetado. Rodar duas vezes sem resetar o
volume entre as rodadas falha em algum `POST` (client_id/tenant code/username já existe).

Credenciais fixas que o script cria (só uso local, nunca usar em produção):

| Papel | Username | Senha |
|---|---|---|
| Platform admin | `admin` | `DevSeed123!` |
| Usuário de teste (`PROMISSORIAS`, perfil `ADMIN`) | `teste_promissorias` | `TesteDev123!` |

## Resumo rápido (depois da primeira vez)

```bash
docker compose up -d
export EMAIL_SENDER=dev@localhost
mvn spring-boot:run
# se o volume do Postgres foi resetado (down -v) e o platform admin/tenant/usuário de
# teste sumiram, rodar de novo (não commitado, ver seção 7):
node scripts/dev-seed.js
# em outro terminal, se quiser o frontend (pasta irmã web/):
cd ../web && ng serve
```

## Problemas comuns

- **`AuthenticationFailedException`/erro 500 ao criar usuário via API**: confirme que o
  MailHog está no ar (`docker compose ps`) — a criação de usuário envia e-mail de
  boas-vindas na mesma transação; se o SMTP falhar, a criação inteira é revertida.
- **Testes de integração falhando em lote após a primeira classe passar**: reinicie a
  suíte — é um comportamento conhecido do container Postgres "singleton" dos testes (ver
  nota em `PROGRESS.md`), não um bug de código.
- **Console Angular não autentica / guard falha silenciosamente**: confira que
  `AUTH_ISSUER`/`spring.security.oauth2.authorizationserver.issuer` resolvem para
  `http://localhost:8080` (já é o default do profile `dev`) — o `angular-oauth2-oidc`
  rejeita silenciosamente se o `issuer` do discovery document não bater com o configurado
  no frontend.
