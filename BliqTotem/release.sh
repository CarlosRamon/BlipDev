#!/bin/bash
set -e

# Lê a versionName do app/build.gradle.kts
VERSION=$(grep "versionName" app/build.gradle.kts | grep -o '"[^"]*"' | tr -d '"')

if [ -z "$VERSION" ]; then
  echo "Erro: não foi possível ler versionName do app/build.gradle.kts"
  exit 1
fi

DEST="releases/$VERSION"

echo "→ Versão: $VERSION"
echo "→ Build release de todos os flavors..."
./gradlew assembleRelease

echo "→ Copiando APKs para $DEST..."
mkdir -p "$DEST"
find app/build/outputs/apk -name "*release*.apk" -exec cp {} "$DEST/" \;

echo "→ Compactando em releases/$VERSION.rar..."
cd releases
rar a "$VERSION.rar" "$VERSION/"
cd ..

echo ""
echo "✓ Concluído: releases/$VERSION.rar"
ls -lh "releases/$VERSION.rar"
