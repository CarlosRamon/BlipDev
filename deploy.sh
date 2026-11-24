#!/bin/bash
set -e

SERVER="root@192.241.151.11"
BACKEND_SRC="./BliqBackend/"
FRONTEND_SRC="./BliqWeb/"

usage() {
  echo "Uso: ./deploy.sh [backend|frontend|all|staging]"
  exit 1
}

deploy_backend() {
  echo "=== Sincronizando backend ==="
  rsync -az --delete \
    --exclude='node_modules' \
    --exclude='dist' \
    --exclude='.env' \
    --exclude='*.log' \
    "$BACKEND_SRC" "$SERVER:/var/www/bliq/backend/"

  echo "=== Buildando e reiniciando backend ==="
  ssh "$SERVER" "bash /var/www/bliq/deploy-backend.sh"
}

deploy_frontend() {
  echo "=== Sincronizando frontend ==="
  rsync -az --delete \
    --exclude='node_modules' \
    --exclude='.next' \
    --exclude='.env.local' \
    --exclude='*.log' \
    "$FRONTEND_SRC" "$SERVER:/var/www/bliq/frontend/"

  echo "=== Buildando e reiniciando frontend ==="
  ssh "$SERVER" "bash /var/www/bliq/deploy-frontend.sh"
}

# Staging na VPS EVEO (Docker): ~/bliq/staging, https://staging.bliqbrasil.com.br
# Envia a árvore de trabalho atual (com o que não estiver commitado).
STAGING_SERVER="redstore@177.136.253.179"
STAGING_DIR="bliq/staging"

deploy_staging() {
  echo "=== Sincronizando staging (VPS EVEO) ==="
  rsync -az --delete \
    --exclude='node_modules' --exclude='dist' --exclude='logs' \
    --exclude='.env' --exclude='.env.*' --exclude='*.log' \
    "$BACKEND_SRC" "$STAGING_SERVER:$STAGING_DIR/backend/"
  rsync -az --delete \
    --exclude='node_modules' --exclude='.next' \
    --exclude='.env' --exclude='.env.*' --exclude='*.log' \
    "$FRONTEND_SRC" "$STAGING_SERVER:$STAGING_DIR/web/"
  rsync -az ./infra/vps-eveo/bliq/compose.yaml "$STAGING_SERVER:$STAGING_DIR/"
  rsync -az ./infra/vps-eveo/bliq/scripts/ "$STAGING_SERVER:$STAGING_DIR/scripts/"

  echo "=== Buildando e reiniciando staging ==="
  ssh "$STAGING_SERVER" "cd $STAGING_DIR && docker compose up -d --build backend web && docker image prune -f >/dev/null && docker compose ps"
}

TARGET="${1:-all}"

if [ "$TARGET" = "staging" ]; then
  deploy_staging
  echo ""
  echo "=== Deploy da staging concluído: https://staging.bliqbrasil.com.br ==="
  exit 0
fi

case "$TARGET" in
  backend)  deploy_backend ;;
  frontend) deploy_frontend ;;
  all)
    deploy_backend
    deploy_frontend
    ;;
  *) usage ;;
esac

echo ""
echo "=== Deploy concluído ==="
ssh "$SERVER" "pm2 status"
