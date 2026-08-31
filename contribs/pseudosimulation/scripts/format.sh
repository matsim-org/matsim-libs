#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(git -C "$MODULE_DIR" rev-parse --show-toplevel 2>/dev/null) || {
	printf 'PSim formatter: cannot locate the Git repository.\n' >&2
	exit 2
}
CHANGED_FILE=$(mktemp "${TMPDIR:-/tmp}/psim-java.XXXXXX") || {
	printf 'PSim formatter: cannot create a temporary changed-file list.\n' >&2
	exit 2
}
trap 'rm -f -- "$CHANGED_FILE"' EXIT

command -v mvn >/dev/null 2>&1 || {
	printf 'PSim formatter: Maven is required but was not found on PATH.\n' >&2
	exit 2
}

git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR -z origin/main -- \
	contribs/pseudosimulation/src/main/java \
	contribs/pseudosimulation/src/test/java >"$CHANGED_FILE"

SPOTLESS_FILES=
while IFS= read -r -d '' path; do
	path=${path#contribs/pseudosimulation/}
	if [[ -z "$SPOTLESS_FILES" ]]; then
		SPOTLESS_FILES=$path
	else
		SPOTLESS_FILES+=",$path"
	fi
done <"$CHANGED_FILE"

if [[ -z "$SPOTLESS_FILES" ]]; then
	printf 'No PSim Java changes from origin/main to format.\n'
	exit 0
fi

mvn --batch-mode \
	-f "$MODULE_DIR/pom.xml" \
	-Ppsim-quality \
	"-DspotlessFiles=$SPOTLESS_FILES" \
	-Dmatsim.preferLocalDtds=true \
	-Dsource.skip \
	spotless:apply

printf '\nFormatting applied. Review and stage the resulting changes.\n'
