# CLAUDE.md

Gestão Financeira Box: projeção de 12 meses de renda e gastos. Visão geral e como subir: [README.md](README.md). Regras de negócio: [PLANO.md](PLANO.md) e [backend/README.md](backend/README.md).

## Fluxo de trabalho (obrigatório; detalhes em [CONTRIBUTING.md](CONTRIBUTING.md))

- **Nunca commite direto em `main` ou `develop`.** Toda demanda (feat/fix/refactor/test/docs/chore/ci) começa numa branch nova criada a partir de `develop`, com prefixo `tipo/assunto`.
- **Um commit por feat/fix/refactor**, em Conventional Commits. Quando há teste associado, os testes também ganham commit (de preferência `test: ...` separado).
- Ao terminar, **abra um PR para `develop`** (`gh pr create --base develop`). O CI roda e, se estiver tudo verde, o PR é aprovado e mergeado automaticamente. Não faça merge manual em `develop`.
- `develop` → `main` é PR **aprovado manualmente pelo dono**. Nunca aprove nem mergeie PR para `main`.
- Versão em beta (SemVer `-beta.N`), fonte única no arquivo `VERSION`; troque só com `scripts/bump-version.sh` e registre no `CHANGELOG.md`.
- Rode os testes localmente antes de abrir o PR.

## Stack e comandos

- Backend: Java 21, Spring Boot 3, JPA, Flyway, PostgreSQL (`backend/`)
- Frontend: React + Vite + TypeScript (`frontend/`)
- Tudo no Docker: `docker compose up -d --build` (precisa de `.env` com `DB_PASSWORD`)
- Só o banco: `docker compose up -d db`
- Backend local: `cd backend && DB_URL=jdbc:postgresql://localhost:5432/gestafinanceira DB_USER=postgres DB_PASSWORD=... mvn spring-boot:run`
- Testes backend: `cd backend && DB_PASSWORD=... mvn test` (domínio sem banco; integração usa `gestafinanceira_test`)
- Frontend: `cd frontend && npm run dev | npm test | npm run build`

## Arquitetura (respeite)

- `domain` (cálculo puro) não depende de Spring, JPA, `application`, `infra` nem `api`.
- **Todo cálculo fica no backend.** O frontend só exibe e envia alterações; não duplique regra de negócio lá.
- Dinheiro sempre `BigDecimal`, `HALF_UP`, 2 casas (igual ao `ROUND` do Excel).
- Tabelas com vigência (INSS, IRPF, deltas, benefícios) usam `vigencia_inicio`/`vigencia_fim`; o cálculo de cada mês usa a vigência daquele mês.
- API em `/api/...`; o frontend usa caminhos relativos (proxy do Vite em dev, nginx no Docker).

## Banco e migrações

- Migrações Flyway em `backend/src/main/resources/db/migration`. **Nunca edite uma migração já aplicada**; crie a próxima (`V5__...`).
- `ddl-auto: validate`: o schema só muda por migração.
- Os dados reais do usuário vivem no volume Docker `dados-pg`.

## Segurança e privacidade

- Nunca commite nem imprima `.env`, dumps (`backup*.sql`, `*.dump`) ou senhas. Só o `.env.example` vai para o git.
- Nunca rode `docker compose down -v`, `docker volume rm` ou `docker system prune --volumes` sem pedido explícito do usuário (apaga o banco). Faça backup antes (ver `docs/persistencia-docker.md`).
- O repositório é privado no GitHub (`pethorian/GestaFinanceiraBox`).

## Convenções

- Idioma do código de domínio, textos da UI e documentação: português do Brasil.
- Mudou regra de cálculo: atualize o teste correspondente (`MotorCalculoTest`, `DecimoFeriasPlanilhaTest`, `SimuladorRealTest`).
- Mudou endpoint: atualize a tabela em `backend/README.md` e `frontend/src/api/api.ts`.
