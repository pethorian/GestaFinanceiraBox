# Gestão Financeira Box

Aplicação web para projetar renda e gastos dos próximos 12 meses (salário, INSS, IRPF, 13º, férias, PLR, bônus, gastos recorrentes/pontuais/parcelados e metas). Derivada da planilha *Simulador de Gastos - Caixa Econômica Federal*.

- **Backend:** Java 21, Spring Boot 3, JPA, Flyway, PostgreSQL → [backend/README.md](backend/README.md)
- **Frontend:** React, Vite, TypeScript → [frontend/README.md](frontend/README.md)
- **Plano e regras de negócio:** [PLANO.md](PLANO.md)
- **Persistência e backup do banco:** [docs/persistencia-docker.md](docs/persistencia-docker.md)

Uma instalação = um usuário, sem login (v1). Todos os cálculos ficam no backend.

## Subir tudo (Docker)

Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/) em execução.

1. Clone o repositório:

   ```bash
   git clone https://github.com/pethorian/GestaFinanceiraBox.git
   cd GestaFinanceiraBox
   ```

2. Crie o `.env` a partir do exemplo e **defina uma senha própria** em `DB_PASSWORD` (veja [Trocar a senha do banco](#trocar-a-senha-do-banco)):

   ```bash
   cp .env.example .env
   ```

   No PowerShell: `Copy-Item .env.example .env`

3. Suba banco, backend e frontend de uma vez:

   ```bash
   docker compose up -d --build
   ```

4. Abra:

   | O quê | URL |
   |---|---|
   | Aplicação | http://localhost:5173 |
   | API | http://localhost:8080/api/dados |
   | Saúde do backend | http://localhost:8080/actuator/health |

Na primeira subida o Flyway cria as tabelas e carrega os dados iniciais.

### Dia a dia

```bash
docker compose ps            # estado dos serviços (db deve ficar "healthy")
docker compose logs -f backend
docker compose stop          # para, mantém tudo
docker compose down          # remove containers, MANTÉM os dados
docker compose up -d --build # sobe de novo / aplica mudanças de código
```

> **Nunca use `docker compose down -v`** a menos que queira apagar o banco. Seus dados ficam no volume `dados-pg`. Detalhes e backup em [docs/persistencia-docker.md](docs/persistencia-docker.md).

### Variáveis do `.env`

| Variável | Padrão | Uso |
|---|---|---|
| `DB_PASSWORD` | (obrigatória) | Senha do Postgres |
| `DB_NAME` / `DB_USER` | `gestafinanceira` / `postgres` | Banco e usuário |
| `DB_PORT` | `5432` | Porta do banco no seu computador |
| `BACKEND_PORT` | `8080` | Porta da API |
| `FRONTEND_PORT` | `5173` | Porta da aplicação |
| `CORS_ORIGINS` | `http://localhost:5173` | Origens aceitas pela API |

## Trocar a senha do banco

O Postgres só lê `DB_PASSWORD` **na primeira criação do volume**. Se você já subiu o projeto, mudar só o `.env` faz o backend falhar com erro de autenticação. É preciso alterar a senha no banco **e** no `.env`.

**Instalação nova (ainda sem dados que importem):** basta definir a senha no `.env` antes do primeiro `docker compose up`.

**Banco já existente (mantendo os dados):**

1. Gere uma senha forte (sem caracteres especiais, para não quebrar o `.env`):

   ```bash
   openssl rand -hex 24
   ```

   No PowerShell: `-join ((1..32) | ForEach-Object { '{0:x}' -f (Get-Random -Max 16) })`

2. Altere a senha dentro do banco (com os containers no ar; a conexão local do container não pede a senha antiga):

   ```bash
   docker compose exec db psql -U postgres -c "ALTER USER postgres PASSWORD 'COLE_A_NOVA_SENHA';"
   ```

   Se mudou `DB_USER`, troque `postgres` pelo seu usuário.

3. Edite o `.env` e coloque a mesma senha em `DB_PASSWORD`.

4. Recrie o backend para ele ler a senha nova:

   ```bash
   docker compose up -d
   ```

5. Confira: `docker compose logs backend` sem erros e http://localhost:8080/actuator/health com `UP`.

**Alternativa que apaga os dados:** `docker compose down -v`, mudar o `.env` e subir de novo. Faça backup antes.

Boas práticas:

- O `.env` nunca vai para o git (já está no `.gitignore`); só o `.env.example`, com valores de exemplo.
- O banco publica a porta no seu computador (`DB_PORT`). O compose a expõe apenas em `127.0.0.1`, mas mesmo assim não use a senha de exemplo.
- Se uma senha vazou ou foi commitada por engano, troque-a pelos passos acima.

## Desenvolvimento sem Docker para o app

Suba só o banco e rode backend e frontend na máquina (hot reload):

```bash
docker compose up -d db

# backend (Java 21 + Maven)
cd backend
DB_URL=jdbc:postgresql://localhost:5432/gestafinanceira DB_USER=postgres DB_PASSWORD=sua-senha mvn spring-boot:run

# frontend (Node 20+), em outro terminal
cd frontend
npm install
npm run dev     # http://localhost:5173, repassa /api para :8080
```

Testes: `mvn test` em `backend/` (veja o README do backend) e `npm test` em `frontend/`.

## Estrutura

```
backend/    Spring Boot (domain / application / infra / api)
frontend/   React + Vite + TS (nginx no container)
docs/       Documentação operacional
docker-compose.yml   db + backend + frontend
.env.example         modelo de configuração (copie para .env)
```

## Dados pessoais

O `.env` e os dumps de banco (`backup*.sql`) estão no `.gitignore` e não devem ser commitados. Seus gastos reais ficam apenas no volume Docker do seu computador.

## Contribuindo e versões

O fluxo de trabalho (branches, commits, PRs e CI) está em [CONTRIBUTING.md](CONTRIBUTING.md). Resumo: nada vai direto para `main` nem `develop`; todo trabalho nasce numa branch, passa pelo CI num PR para `develop`, e `develop` chega à `main` por PR aprovado manualmente.

O projeto está em **beta** e segue [SemVer](https://semver.org/lang/pt-BR/) com sufixo `-beta.N` (ex.: `0.1.0-beta.1`). A versão atual está no arquivo [VERSION](VERSION) e o histórico em [CHANGELOG.md](CHANGELOG.md).
