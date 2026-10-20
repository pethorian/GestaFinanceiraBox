# Persistência dos dados no Docker

Os dados do Postgres ficam no **volume nomeado** `dados-pg`, montado em
`/var/lib/postgresql/data`. O volume é independente do container: apagar ou recriar o
container NÃO apaga os dados. (No Compose o nome real é `<projeto>_dados-pg`; o projeto
padrão é o nome da pasta, p.ex. `gestafinanceirabox_dados-pg`. **Renomear a pasta do projeto
cria um volume novo e vazio** - o antigo continua existindo; use `-p <nome-antigo>` ou
restaure um backup.)

## Configuração

Copie `.env.example` para `.env` e defina `DB_PASSWORD` (obrigatório). Opcionais:
`DB_PORT` (porta do banco no host, padrão 5432) e `BACKEND_PORT` (padrão 8080) e `FRONTEND_PORT` (padrão 5173).

## Subir tudo (db + backend + frontend)

`docker compose up -d --build` sobe os três serviços. O frontend (nginx) fica em
`http://localhost:${FRONTEND_PORT:-5173}` e repassa `/api` ao backend; se mudar `FRONTEND_PORT`,
ajuste também `CORS_ORIGINS` no `.env`.

## Subir

```
docker compose up -d --build
docker compose ps        # db deve ficar "healthy"
```

O serviço `db` usa `restart: unless-stopped` (volta sozinho após reboot/queda do Docker,
a menos que você o tenha parado de propósito).

## Derrubar SEM perder dados

```
docker compose down      # remove containers e rede; MANTÉM o volume
docker compose stop      # só para
docker compose restart db
```

Ao subir de novo, o Flyway apenas valida as migrações já aplicadas
("Schema is up to date. No migration necessary"): as seeds V2/V3 NÃO são reexecutadas e
nada é sobrescrito. O backend usa `ddl-auto: validate` (não altera tabelas) e não há
`flyway clean` habilitado.

## O que APAGA os dados (cuidado)

- `docker compose down -v` / `--volumes`
- `docker volume rm <projeto>_dados-pg` e `docker volume prune` / `docker system prune --volumes`
  (se o volume estiver sem container associado)
- Reset de dados do Docker Desktop (Troubleshoot > Clean / Purge data) ou desinstalação

## Backup e restauração

Backup (arquivo `.sql` no host):

```
docker compose exec -T db pg_dump -U postgres -d gestafinanceira --clean --if-exists > backup.sql
```

Restauração (no banco do compose em execução; usuário/banco conforme seu `.env`):

```
docker compose exec -T db psql -U postgres -d gestafinanceira < backup.sql
```

Em PowerShell, prefira `cmd /c "docker compose exec -T db pg_dump ... > backup.sql"` para
evitar a conversão de codificação do redirecionamento. Em um volume zerado, suba o
backend uma vez (cria o schema) antes de restaurar, ou restaure direto: o dump com
`--clean --if-exists` recria as tabelas.

Faça backup antes de `down -v`, de atualizar a versão major do Postgres ou de mexer no volume.

## Como foi verificado (Docker 29.9.1, Compose 5.6.0, projeto de teste `-p gfbtest`, DB na porta 5433)

1. Subiu `db` + backend (jar na porta 8081); gravou via API: `POST /api/lancamentos/novo`,
   `POST /api/metas/novo` e `PUT /api/perfil` com `saldoInicial=7777.77`.
2. `docker compose down` (sem `-v`) e `up -d`: 8 lançamentos, 4 metas, saldo 7777.77, 3 migrações Flyway (inalterado).
3. `docker kill` (SIGKILL, log: "database system was not properly shut down; automatic recovery"),
   `restart` e `stop`/`start`: dados idênticos.
4. Novo backend contra o mesmo volume: Flyway "up to date. No migration necessary" e
   `GET /api/dados` byte a byte igual ao anterior.
5. `pg_dump`, `down -v` (volume removido, banco novo com 0 tabelas) e restauração: dados recuperados.
