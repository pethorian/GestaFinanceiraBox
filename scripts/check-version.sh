#!/usr/bin/env bash
# Confere se VERSION, backend/pom.xml e frontend/package.json estão alinhados.
# Com --changelog, exige também a seção "## [VERSION]" no CHANGELOG.md.
set -euo pipefail
cd "$(dirname "$0")/.."

v=$(tr -d '[:space:]' < VERSION)
pom=$(sed -n '/<artifactId>gestafinanceira-backend<\/artifactId>/{n;s#.*<version>\(.*\)</version>.*#\1#p;q}' backend/pom.xml | tr -d '\r')
pkg=$(sed -n 's/^  "version": "\(.*\)",/\1/p' frontend/package.json | head -1 | tr -d '\r')

erro=0
if ! [[ "$v" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-beta\.[0-9]+)?$ ]]; then
  echo "::error::VERSION '$v' fora do padrão MAJOR.MINOR.PATCH[-beta.N]"; erro=1
fi
[ "$pom" = "$v" ] || { echo "::error::backend/pom.xml tem '$pom', VERSION tem '$v'"; erro=1; }
[ "$pkg" = "$v" ] || { echo "::error::frontend/package.json tem '$pkg', VERSION tem '$v'"; erro=1; }

if [ "${1:-}" = "--changelog" ]; then
  grep -qF "## [$v]" CHANGELOG.md || { echo "::error::CHANGELOG.md sem a seção '## [$v]'"; erro=1; }
fi

[ "$erro" -eq 0 ] && echo "Versão $v consistente."
exit "$erro"
