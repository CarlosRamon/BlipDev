#!/bin/bash
# Gera o PDF da documentação da API do franqueado a partir de api-franqueado.html.
# A versão e a data vêm das metas doc-version / doc-date do HTML: ao revisar a
# documentação, atualize as duas (e o texto da capa e do rodapé de impressão)
# e rode este script de novo. Cada versão sai num arquivo próprio.
#
# Uso: bash docs/gerar-pdf.sh
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$DIR/api-franqueado.html"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

VERSAO="$(sed -n 's/.*<meta name="doc-version" content="\([^"]*\)".*/\1/p' "$SRC")"
DATA="$(sed -n 's/.*<meta name="doc-date" content="\([^"]*\)".*/\1/p' "$SRC")"
[ -n "$VERSAO" ] && [ -n "$DATA" ] || { echo "Metas doc-version/doc-date ausentes em $SRC" >&2; exit 1; }

OUT="$DIR/API_Bliq_Franqueados_v${VERSAO}_${DATA}.pdf"

# O HTML é escrito sem <!doctype> (o publicador de artifacts adiciona); sem ele
# o Chrome renderiza em quirks mode, então o wrapper vai num arquivo temporário.
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
{ printf '<!doctype html>\n<html lang="pt-BR">\n<meta charset="utf-8">\n'; cat "$SRC"; } > "$TMP/doc.html"

"$CHROME" --headless=new --disable-gpu --no-pdf-header-footer \
  --virtual-time-budget=10000 \
  --print-to-pdf="$OUT" "file://$TMP/doc.html" 2>/dev/null

echo "PDF gerado: $OUT"
