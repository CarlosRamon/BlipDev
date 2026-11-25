#!/bin/bash
# Deploy da Bliq na VPS EVEO (Docker, ver infra/vps-eveo/bliq/README.md).
#   bash deploy.sh all        # produção: backend + web  → https://app.bliqbrasil.com.br
#   bash deploy.sh backend    # produção: só backend
#   bash deploy.sh frontend   # produção: só web
#   bash deploy.sh staging    # staging: backend + web   → https://staging.bliqbrasil.com.br
#
# Envia a árvore de trabalho atual, inclusive o que não estiver commitado.
# Migrations pendentes rodam no boot do backend (prisma migrate deploy).
# Até 2026-11-24 a produção ficava em root@192.241.151.11 (PM2), hoje parado.
set -e

VPS="redstore@177.136.253.179"
BACKEND_SRC="./BliqBackend/"
FRONTEND_SRC="./BliqWeb/"

usage() {
  echo "Uso: ./deploy.sh [backend|frontend|all|staging]"
  exit 1
}

avisar_nao_commitado() {
  local repo=$1
  if [ -n "$(git -C "$repo" status --porcelain --untracked-files=normal)" ]; then
    echo "!!! $repo tem alterações não commitadas — vão junto no deploy:"
    git -C "$repo" status --short | sed 's/^/    /'
  fi
}

sync_backend() {
  local dir=$1
  echo "=== Sincronizando backend → $dir ==="
  avisar_nao_commitado "$BACKEND_SRC"
  rsync -az --delete \
    --exclude='node_modules' --exclude='dist' --exclude='logs' \
    --exclude='.env' --exclude='.env.*' --exclude='*.log' \
    "$BACKEND_SRC" "$VPS:$dir/backend/"
}

sync_web() {
  local dir=$1
  echo "=== Sincronizando web → $dir ==="
  avisar_nao_commitado "$FRONTEND_SRC"
  rsync -az --delete \
    --exclude='node_modules' --exclude='.next' \
    --exclude='.env' --exclude='.env.*' --exclude='*.log' \
    "$FRONTEND_SRC" "$VPS:$dir/web/"
}

sync_infra() {
  local dir=$1
  rsync -az ./infra/vps-eveo/bliq/compose.yaml "$VPS:$dir/"
  rsync -az ./infra/vps-eveo/bliq/scripts/ "$VPS:$dir/scripts/"
}

subir() {
  local dir=$1; shift
  echo "=== Buildando e reiniciando ($*) em $dir ==="
  ssh "$VPS" "cd $dir && docker compose up -d --build $* && docker image prune -f >/dev/null && docker compose ps"
}

TARGET="${1:-all}"

case "$TARGET" in
  backend)  D=bliq/prod;    sync_infra $D; sync_backend $D;            subir $D backend ;;
  frontend) D=bliq/prod;    sync_infra $D; sync_web $D;                subir $D web ;;
  all)      D=bliq/prod;    sync_infra $D; sync_backend $D; sync_web $D; subir $D backend web ;;
  staging)  D=bliq/staging; sync_infra $D; sync_backend $D; sync_web $D; subir $D backend web ;;
  *) usage ;;
esac

echo ""
echo "=== Deploy concluído ($TARGET) ==="
