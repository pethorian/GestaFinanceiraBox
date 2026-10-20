#!/usr/bin/env bash
# Uso: scripts/bump-version.sh 0.1.0-beta.2
# Atualiza VERSION, backend/pom.xml, frontend/package.json e package-lock.json.
set -euo pipefail
cd "$(dirname "$0")/.."

v="${1:?uso: $0 MAJOR.MINOR.PATCH[-beta.N]}"
[[ "$v" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-beta\.[0-9]+)?$ ]] || { echo "Versão inválida: $v" >&2; exit 1; }

printf '%s\n' "$v" > VERSION
sed -i '/<artifactId>gestafinanceira-backend<\/artifactId>/{n;s#<version>[^<]*</version>#<version>'"$v"'</version>#}' backend/pom.xml
V="$v" node -e '
  const fs = require("fs");
  for (const f of ["frontend/package.json", "frontend/package-lock.json"]) {
    const j = JSON.parse(fs.readFileSync(f, "utf8"));
    j.version = process.env.V;
    if (j.packages && j.packages[""]) j.packages[""].version = process.env.V;
    fs.writeFileSync(f, JSON.stringify(j, null, 2) + "\n");
  }
'
echo "Versão atualizada para $v. Registre no CHANGELOG.md."
