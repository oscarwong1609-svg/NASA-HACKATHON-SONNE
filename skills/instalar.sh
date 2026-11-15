#!/usr/bin/env bash
# Instala las skills de esta carpeta en la carpeta personal de Claude Code (~/.claude/skills).
# Uso, desde la raiz del repositorio:  bash skills/instalar.sh
set -euo pipefail
origen="$(cd "$(dirname "$0")" && pwd)"
destino="$HOME/.claude/skills"
mkdir -p "$destino"

n=0
for carpeta in "$origen"/*/; do
  nombre="$(basename "$carpeta")"
  mkdir -p "$destino/$nombre"
  cp -R "$carpeta". "$destino/$nombre/"
  echo "Instalada: $nombre"
  n=$((n + 1))
done
echo
echo "$n skills instaladas en $destino"
echo "Cierra y vuelve a abrir Claude Code para que las cargue. Los plugins se instalan aparte (ver skills/README.md)."
