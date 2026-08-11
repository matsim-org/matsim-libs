#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)

command -v mvn >/dev/null 2>&1 || {
	printf 'PSim formatter: Maven is required but was not found on PATH.\n' >&2
	exit 2
}

mvn --batch-mode \
	-f "$MODULE_DIR/pom.xml" \
	-Ppsim-quality \
	-Dmatsim.preferLocalDtds=true \
	-Dsource.skip \
	spotless:apply

printf '\nFormatting applied. Review and stage the resulting changes.\n'
