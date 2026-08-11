#!/usr/bin/env bash

set -uo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(git -C "$MODULE_DIR" rev-parse --show-toplevel 2>/dev/null) || {
	printf 'PSim quality: cannot locate the Git repository.\n' >&2
	exit 2
}
MVN=(mvn --batch-mode -f "$MODULE_DIR/pom.xml" -Ppsim-quality -Dmatsim.preferLocalDtds=true -Dsource.skip)

print_help() {
	cat <<'EOF'
Usage: quality.sh [--pre-commit]

Runs PSim formatting, lint, static-analysis, unit-test, and coverage gates.
The pre-commit mode skips commits unrelated to PSim and rejects partially
staged PSim files so the working tree cannot be mistaken for the staged commit.
EOF
}

is_relevant_path() {
	case "$1" in
		contribs/pseudosimulation/src/*.java | contribs/pseudosimulation/src/**/*.java | \
		contribs/pseudosimulation/pom.xml | contribs/pseudosimulation/quality/* | \
		contribs/pseudosimulation/scripts/* | contribs/pseudosimulation/QUALITY.md)
			return 0
			;;
		*) return 1 ;;
	esac
}

check_staged_files() {
	local path
	local staged_file
	local -a staged=()
	local -a relevant=()

	staged_file=$(mktemp "${TMPDIR:-/tmp}/psim-staged.XXXXXX") || {
		printf 'PSim quality: cannot create a temporary staged-file list.\n' >&2
		exit 2
	}
	if ! git -C "$REPO_ROOT" diff --cached --name-only --diff-filter=ACMRD -z >"$staged_file"; then
		rm -f -- "$staged_file"
		printf 'PSim quality: cannot inspect staged files.\n' >&2
		exit 2
	fi
	mapfile -d '' -t staged <"$staged_file"
	rm -f -- "$staged_file"
	for path in "${staged[@]}"; do
		if is_relevant_path "$path"; then
			relevant+=("$path")
		fi
	done

	if ((${#relevant[@]} == 0)); then
		printf 'PSim quality: no relevant staged files; skipping.\n'
		exit 0
	fi

	for path in "${relevant[@]}"; do
		if ! git -C "$REPO_ROOT" diff --quiet -- "$path"; then
			printf 'PSim quality: partially staged file detected: %s\n' "$path" >&2
			printf 'Stage or stash its remaining changes, then retry the commit.\n' >&2
			exit 1
		fi
	done
}

run_stage() {
	local name=$1
	local remedy=$2
	shift 2

	printf '\n==> %s\n' "$name"
	if "$@"; then
		printf 'PASS: %s\n' "$name"
	else
		printf '\nFAIL: %s\n%s\n' "$name" "$remedy" >&2
		exit 1
	fi
}

case "${1:-}" in
	--pre-commit) check_staged_files ;;
	-h | --help) print_help; exit 0 ;;
	"") ;;
	*) print_help >&2; exit 2 ;;
esac

command -v mvn >/dev/null 2>&1 || {
	printf 'PSim quality: Maven is required but was not found on PATH.\n' >&2
	exit 2
}
command -v java >/dev/null 2>&1 || {
	printf 'PSim quality: Java 25 is required but was not found on PATH.\n' >&2
	exit 2
}

run_stage \
	'Dependency preparation' \
	'Fix the Maven compilation/dependency error shown above.' \
	mvn --batch-mode -pl contribs/pseudosimulation -am -DskipTests -Dsource.skip install -f "$REPO_ROOT/pom.xml"

run_stage \
	'Formatting' \
	"Run $SCRIPT_DIR/format.sh, review the changes, and stage them." \
	"${MVN[@]}" spotless:check

run_stage \
	'Lint' \
	"Resolve the Checkstyle violations listed above; rules are in $MODULE_DIR/quality/checkstyle.xml." \
	"${MVN[@]}" checkstyle:check

run_stage \
	'Static analysis' \
	"Resolve the high-priority SpotBugs findings above; XML details are in $MODULE_DIR/target/spotbugsXml.xml." \
	"${MVN[@]}" compile spotbugs:check

run_stage \
	'Unit tests and coverage' \
	"Fix the failing test or add coverage. Reports are under $MODULE_DIR/target/surefire-reports and target/site/jacoco." \
	"${MVN[@]}" verify

printf '\nPSim quality gate passed.\n'
