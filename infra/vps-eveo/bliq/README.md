# Bliq na VPS EVEO

VPS `177.136.253.179` (`ssh redstore@177.136.253.179`), a mesma da RedStore.
Detalhes da máquina e do NAT: memória `project-vps-eveo-sp` do projeto redstore.
O proxy HTTPS é o `~/edge` compartilhado (Caddy na 8443, ver `~/edge/README.md`).

```
Cloudflare (proxied, Full strict) ─► :8443 edge-caddy ─┬─ /api, /health → bliq-<amb>-backend:3000 ─► bliq-<amb>-mysql
                                                       └─ resto         → bliq-<amb>-web:3002
```

| Ambiente | Diretório        | Host                         | Estado                         |
|----------|------------------|------------------------------|--------------------------------|
| staging  | `~/bliq/staging` | `staging.bliqbrasil.com.br`  | no ar desde 2026-11-24         |
| prod     | `~/bliq/prod`    | `app.bliqbrasil.com.br`      | a criar (hoje: 192.241.151.11) |

## Arquivos

Este diretório é a cópia versionada; na VPS cada ambiente tem:

```
~/bliq/<amb>/
├── compose.yaml        # igual a este
├── .env                # AMBIENTE + senhas do MySQL (600) — ver env.example
├── backend.env         # JWT_SECRET, CORS_ORIGIN, ANTHROPIC_API_KEY... (600) — ver backend.env.example
├── backend/  web/      # código enviado pelo deploy.sh (Dockerfile dentro de cada repo)
├── data/mysql/         # dados do MySQL 8.0.45
├── logs/               # /var/log/bliq do backend (backend-app-errors.log)
├── backups/            # dumps .sql.gz
└── scripts/backup.sh, restore.sh
```

O host do edge fica em `~/edge/sites/<host>.caddy` (cópia em `edge-sites/`).
Ele só aceita a Cloudflare e redes privadas (acesso direto à origem → 403) e
repassa `CF-Connecting-IP` como `X-Forwarded-For`. O backend confia na rede
`edge` via `PROXIES_CONFIAVEIS_EXTRA`, então o rate limit continua por cliente.

## Operação

```bash
bash deploy.sh staging                                   # da raiz do BlipDev
ssh redstore@177.136.253.179
cd ~/bliq/staging
docker compose ps
docker compose logs -f --tail 100 backend
tail -f logs/backend-app-errors.log                      # só erros inesperados
docker exec -it bliq-staging-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" bliq'
scripts/backup.sh
scripts/restore.sh backups/<arquivo>.sql.gz              # pede o nome do ambiente
```

O backend roda `prisma migrate deploy` a cada subida do container.

## Recarregar a staging com o banco de produção

O banco de produção está no MySQL gerenciado da DigitalOcean (nyc1), que só
aceita o droplet antigo. O dump sai de lá direto para a VPS:
ver o histórico da sessão de 2026-11-24 (mysqldump `--single-transaction
--set-gtid-purged=OFF --no-tablespaces` a partir de `/var/www/bliq/backend/.env`).

## Virada de produção (resumo)

1. Criar `~/bliq/prod` com `AMBIENTE=prod`, `JWT_SECRET` **idêntico** ao do
   servidor antigo, `CORS_ORIGIN=https://app.bliqbrasil.com.br`, sem limite do assistente.
2. Site `app.bliqbrasil.com.br.caddy` apontando para `bliq-prod-*`, cert Origin CA.
3. Na janela: `pm2 stop all` no antigo → dump final → restore em prod → subir.
4. Cloudflare: A de `app` → `177.136.253.179`, Origin Rule 8443, Full (strict).
5. Validar totem (heartbeat, ciclo), painel e `logs/backend-app-errors.log`.
6. Backup diário em cron + cópia fora da VPS; antigo parado como rollback.
