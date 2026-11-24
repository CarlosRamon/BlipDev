#!/usr/bin/env bash
# Restaura um dump .sql.gz no banco `bliq` do ambiente (SOBRESCREVE as tabelas).
# Uso: ~/bliq/<ambiente>/scripts/restore.sh backups/arquivo.sql.gz
set -euo pipefail

ARQ="${1:?uso: restore.sh <arquivo.sql.gz>}"
DIR="$(cd "$(dirname "$0")/.." && pwd)"
ARQ="$(cd "$(dirname "$ARQ")" && pwd)/$(basename "$ARQ")"
cd "$DIR"
set -a; . ./.env; set +a

read -r -p "Restaurar $ARQ em bliq-${AMBIENTE}-mysql? Digite o ambiente para confirmar: " OK
[ "$OK" = "$AMBIENTE" ] || { echo "cancelado"; exit 1; }

gunzip -c "$ARQ" | docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "bliq-${AMBIENTE}-mysql" mysql -uroot bliq
echo "restaurado: $ARQ"
