---
name: build-auth-server-v2
description: Implementa o Auth Server V2 multi-tenant seguindo "# Plano de Implementação — Auth Server v2.md" — trabalha item a item, escreve testes para tudo que implementa, roda a suíte antes de cada commit, e só commita funcionalidade fechada com testes verdes. Use quando o usuário pedir para continuar, avançar, ou trabalhar na implementação do auth-server-v2 (ex: "continua a implementação", "próxima fase", "implementa o domínio").
---

# Build Auth Server V2

Implementa o projeto descrito em `# Plano de Implementação — Auth Server v2.md` (raiz do repo). Esse
documento é a fonte da verdade sobre arquitetura, modelo de dados, padrões de código e ordem das
fases (seção 10) — **releia a seção relevante do plano antes de implementar cada fase**, não confie
apenas na memória desta skill.

Esta skill não é "implementar tudo de uma vez". Cada invocação avança um pedaço coerente de trabalho
(um VO, uma entidade, um serviço, um endpoint) até fechar um item do checklist, com testes verdes, e
então commita. Sessões futuras retomam de onde pararam.

## Estado e retomada

Existe (ou crie na primeira execução) um arquivo `PROGRESS.md` na raiz do repo, espelhando os
checklists da seção 10 do plano, fase por fase. Antes de fazer qualquer coisa:

1. Leia `PROGRESS.md`. Se não existir, crie-o copiando os checklists da seção 10 do plano
   (Fase 0 a Fase 11), todos desmarcados.
2. Identifique a primeira fase com itens não marcados — essa é a fase corrente.
3. **Nunca pule para uma fase seguinte com itens pendentes na fase corrente.** O plano é explícito:
   cada fase deve estar verde nos testes antes da próxima (seção 10, introdução).
4. Dentro da fase corrente, pegue o primeiro item não marcado como a unidade de trabalho desta rodada.

Se o usuário pedir explicitamente para pular a ordem (ex: "implementa só a parte de JWT agora"),
respeite o pedido, mas registre em `PROGRESS.md` que a ordem padrão foi alterada por pedido explícito.

## Ciclo de trabalho por item

Para cada item do checklist que for fechado nesta rodada, siga exatamente esta sequência — não pule
etapas e não acumule múltiplos itens não testados antes de rodar testes:

1. **Implementar** o item, seguindo à risca os padrões obrigatórios da seção 6 do plano (Value
   Object, entidade de domínio, binding, geração de ID, adapter de persistência, tratamento de erro)
   e as regras de dependência da seção 5.1 (domain não importa nada de framework, etc).
2. **Escrever testes** para o que foi implementado — sem exceção. Isso vale para VOs, entidades,
   serviços de domínio, use cases, adapters de persistência, controllers e configuração de segurança.
   Cobrir o caminho feliz **e** as violações de invariante (mensagens de erro incluídas). Para itens
   de isolamento multi-tenant (seção 8.3), os testes ali listados são obrigatórios, não opcionais.
3. **Rodar a suíte de testes inteira** (`mvn test`, ou `mvn verify` se houver Testcontainers/ArchUnit
   envolvidos na fase), não só os testes novos — para pegar regressão em itens já commitados.
4. Se algum teste falhar: corrija até ficar verde. Nunca commite com testes vermelhos, quebrados ou
   desabilitados (`@Disabled`) para "resolver depois".
5. Só depois da suíte verde: **marque o item em `PROGRESS.md`** e faça o commit (ver formato abaixo).
6. Passe para o próximo item. Repita.

Regra dura: **um commit nunca mistura "implementação sem teste" com "teste depois, em outro commit".**
Implementação e os testes daquela funcionalidade entram juntos no mesmo commit, porque o commit só
acontece depois que os testes já passaram.

## Commits

- Um commit por item de checklist fechado (ou por grupo pequeno e coerente de itens estritamente
  relacionados, ex: todos os VOs triviais de uma leva — mas nunca uma fase inteira num commit só).
- Formato da mensagem: Conventional Commits, em pt-BR, escopo pela camada/fase:
  `feat(domain): adiciona value object TenantCode` ou `feat(persistence): adiciona UserRepositoryImpl com queries escopadas por tenant`.
  Corpo opcional de 1-2 linhas se o "porquê" não for óbvio (ex: por que uma invariante existe).
- Inclua a atualização de `PROGRESS.md` no mesmo commit da funcionalidade que ela fecha — não em
  commit separado.
- Nunca use `git commit --amend`, `--no-verify`, nem force-push.
- Se um hook de pre-commit falhar, corrija a causa raiz e crie um **novo** commit — nunca `--no-verify`.

## Regras arquiteturais não negociáveis (lembrete rápido — o plano é a fonte completa)

- Isolamento multi-tenant é a prioridade #1 do projeto (seção 1.2, 8, 3.3). Qualquer repositório ou
  query que toque dado de tenant recebe `TenantId` explícito como primeiro parâmetro — nunca um
  `TenantContext` implícito em ThreadLocal (rejeitado deliberadamente na seção 8.2).
- `application/port/out` para interfaces de repositório (não `domain/repository` — é a única
  divergência intencional do projeto de referência, seção 2.2).
- IDs via TSID gerado na aplicação (`IdGeneratorPort`), nunca `BIGSERIAL`/`@GeneratedValue`.
- Token carrega só códigos de perfil. Nenhuma tabela ou lógica de permissão/menu/ação — se sentir
  necessidade disso, pare e releia a seção 1.2 antes de continuar.
- Mensagens de domínio em português; nomes de código e logs em inglês; mensagens de commit em pt-BR.
- Nenhuma credencial em `application*.yml` versionado (armadilha explícita do projeto de referência,
  seção 12) — variáveis de ambiente / `application-local.yml` no `.gitignore`.
- FKs compostas do schema (seção 4.4) são o mecanismo mais importante de segurança do projeto — não
  simplificar/remover em nome de "limpar o schema".

## Antes de começar (só na primeira execução, Fase 0)

- Se o diretório não for um repositório git ainda, rode `git init` e faça o primeiro commit
  (estrutura vazia / `.gitignore` / `README.md`) antes de qualquer código — é a base para todos os
  commits incrementais seguintes.
- Confirme a stack (Java 25, Spring Boot 4.x, Maven) e crie a estrutura de pacotes da seção 5 do
  plano vazia, com um `ArchUnitTest` inicial que já falha até as regras existirem — vira o guarda-
  -chuva de todas as fases seguintes.

## Quando parar e perguntar

- Decisão de negócio/segurança não coberta pelo plano (ex: TTL não especificado, política ambígua).
  As decisões da seção 2 **não devem ser revisitadas** — não pergunte sobre elas, siga como está.
- Comando destrutivo (reset de banco em ambiente com dados, force-push, etc) — sempre confirme antes.
- Ambiguidade real sobre qual item do `PROGRESS.md` o usuário quer priorizar quando ele pede algo
  fora de ordem.

Fora isso, prefira agir e reportar no fim da rodada o que foi fechado (itens marcados + commits
criados), em vez de pedir aprovação passo a passo.
