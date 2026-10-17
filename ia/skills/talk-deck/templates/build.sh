#!/usr/bin/env bash
set -euo pipefail

Root="$(cd "$(dirname "$0")" && pwd)"
Deck="$Root/slide-deck.md"
GeneratedDeck="$Root/.slide-deck.generated.md"
Theme="$Root/theme.css"
PrepareMermaid="$Root/prepare-mermaid.mjs"
Dist="$Root/dist"
Target="${1:-all}"

if [[ ! -f "$Deck" ]]; then
  echo "slide-deck.md is missing in $Root" >&2
  exit 1
fi
if [[ ! -f "$Theme" ]]; then
  echo "theme.css is missing in $Root" >&2
  exit 1
fi
if [[ ! -f "$PrepareMermaid" ]]; then
  echo "prepare-mermaid.mjs is missing in $Root" >&2
  exit 1
fi

mkdir -p "$Dist"
node "$PrepareMermaid"
if [[ ! -f "$GeneratedDeck" ]]; then
  echo "Mermaid preparation failed." >&2
  exit 1
fi

if [[ -d "$Root/assets" ]]; then
  rm -rf "$Dist/assets"
  cp -R "$Root/assets" "$Dist/assets"
fi

run_marp() {
  local output="$1"
  if ! npx --yes @marp-team/marp-cli@latest "$GeneratedDeck" --theme-set "$Theme" --theme talk --allow-local-files -o "$output"; then
    echo "Marp failed while writing $output. PDF export needs Chrome, Edge, or Chromium. Set CHROME_PATH to the browser executable if needed." >&2
    exit 1
  fi
}

case "$Target" in
  all)
    run_marp "$Dist/slide-deck.html"
    run_marp "$Dist/slide-deck.pdf"
    ;;
  html)
    run_marp "$Dist/slide-deck.html"
    ;;
  pdf)
    run_marp "$Dist/slide-deck.pdf"
    ;;
  *)
    echo "Usage: ./build.sh [all|html|pdf]" >&2
    exit 1
    ;;
esac
