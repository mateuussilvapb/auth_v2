# Conflito de porta: Postgres nativo do Windows na 5432

Armadilha específica de máquinas Windows que já têm um Postgres instalado como serviço
nativo (fora do Docker). Documentado também no repo irmão `sistema_promissorias`
(`docs/06-ambiente-auth-server-local.md` e `docs/08-acessar-postgres-nos-containers.md`) —
mantenha os dois em sincronia se o diagnóstico ou a correção mudarem.

## Sintoma

`mvn spring-boot:run` falha no boot com:

```
FATAL: autenticação do tipo senha falhou para o usuário "authserver"
```

mesmo com `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` corretos e batendo com o
`docker-compose.yml`. `docker compose ps` mostra o container do Postgres `Up` e a porta
`5432` publicada normalmente — isso **não** garante que o tráfego chega ao container.

## Causa

Um Postgres nativo do Windows (serviço, ex: `postgresql-x64-17`) também escuta em
`0.0.0.0:5432`, junto do container Docker (`auth-server-v2-postgres-1`) publicado na mesma
porta via port-forward do Docker Desktop (`com.docker.backend`, em `[::]:5432`, IPv4/IPv6
como sockets distintos). Dependendo de qual endereço `localhost` resolve primeiro, a conexão
cai no Postgres nativo (usuário/senha diferentes) em vez do container — e o log do container
Docker não registra nada, porque a conexão nunca chega até ele.

## Diagnóstico rápido

```powershell
Get-NetTCPConnection -LocalPort 5432 -State Listen |
  Select-Object OwningProcess |
  ForEach-Object { Get-Process -Id $_.OwningProcess | Select-Object ProcessName, Path }
```

Se aparecer um `postgres.exe` que não pertence ao Docker (verifique com
`Get-Service | Where-Object Name -match postgres`), é isso.

## Correção (sem tocar nos arquivos versionados)

**Não mude** o default de `application-dev.yml` nem a porta publicada em
`docker-compose.yml` — eles devem continuar batendo com um ambiente "limpo", já que outras
máquinas podem não ter esse conflito. A correção fica só nesta máquina, via
`docker-compose.override.yml` (gitignored):

```yaml
# api/docker-compose.override.yml — não versionado, específico desta máquina
services:
  postgres:
    ports:
      - "15432:5432"
```

`docker compose` carrega esse override automaticamente junto do `docker-compose.yml`. Depois,
aponte a aplicação para a porta alternativa ao rodar localmente:

```bash
docker compose up -d
DB_URL="jdbc:postgresql://localhost:15432/authserver" mvn spring-boot:run
```

## Ver também

- `sistema_promissorias/docs/06-ambiente-auth-server-local.md` §1 — mesmo problema, runbook
  completo (inclui como resetar a senha se o volume já existia com credencial diferente).
- `sistema_promissorias/docs/08-acessar-postgres-nos-containers.md` — tabela de referência
  rápida de portas/credenciais dos dois bancos.
