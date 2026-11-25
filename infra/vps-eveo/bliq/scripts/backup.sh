#!/usr/bin/env bash
# Dump do MySQL do ambiente para backups/, mantendo 14 dias.
# Uso: ~/bliq/<ambiente>/scripts/backup.sh   (cron: 15 4 * * *)
set -euo pipefail
umask 077   # dump tem dados de clientes

DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$DIR"
set -a; . ./.env; set +a

mkdir -p backups
ARQ="backups/bliq-${AMBIENTE}-$(date -u +%Y%m%d-%H%M%S).sql.gz"

docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "bliq-${AMBIENTE}-mysql" \
  mysqldump -uroot --single-transaction --routines --triggers --hex-blob \
  --set-gtid-purged=OFF --no-tablespaces bliq \
  | gzip > "$ARQ.tmp"
mv "$ARQ.tmp" "$ARQ"

find backups -name "bliq-${AMBIENTE}-*.sql.gz" -mtime +14 -delete
echo "$(date -u +%FT%TZ) ok $ARQ $(du -h "$ARQ" | cut -f1)"
