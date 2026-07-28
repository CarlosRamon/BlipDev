#!/bin/bash
set -e

SERVER="root@192.241.151.11"
BACKEND_SRC="./BliqBackend/"
FRONTEND_SRC="./BliqWeb/"

usage() {
  echo "Uso: ./deploy.sh [backend|frontend|all]"
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

TARGET="${1:-all}"

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
