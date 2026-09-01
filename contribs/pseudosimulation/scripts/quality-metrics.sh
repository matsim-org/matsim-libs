#!/usr/bin/env bash

set -uo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
BASELINE_FILE=${PSIM_METRICS_BASELINE:-$MODULE_DIR/quality/metrics-baseline.properties}
TARGET_DIR=${PSIM_METRICS_TARGET:-$MODULE_DIR/target}
JACOCO_CSV=$TARGET_DIR/site/jacoco/jacoco.csv
SUREFIRE_DIR=$TARGET_DIR/surefire-reports
CHECKSTYLE_XML=$TARGET_DIR/checkstyle-result.xml
SPOTBUGS_XML=$TARGET_DIR/spotbugsXml.xml

METRIC_KEYS=(
	line_coverage_basis_points
	branch_coverage_basis_points
	tests
	test_failures
	test_errors
	test_skipped
	checkstyle_violations
	spotbugs_findings
)

print_help() {
	cat <<'EOF'
Usage: quality-metrics.sh [--check | --report | --ratchet]

  --check    Print the scorecard and fail if a metric regressed (default).
  --report   Print the scorecard without enforcing the baseline.
  --ratchet  Refuse regressions, then replace the baseline with current values.

Reports must already exist; run scripts/quality.sh to generate them.
EOF
}

fail_missing() {
	printf 'PSim metrics: required report is missing: %s\n' "$1" >&2
	printf 'Run %s/quality.sh to regenerate all reports.\n' "$SCRIPT_DIR" >&2
	exit 2
}

require_file() {
	[[ -f "$1" ]] || fail_missing "$1"
}

read_attribute_total() {
	local attribute=$1
	local total=0
	local value
	local file
	local found=0
	for file in "$SUREFIRE_DIR"/TEST-*.xml; do
		[[ -f "$file" ]] || continue
		value=$(sed -n "s/.* $attribute=\"\([0-9][0-9]*\)\".*/\1/p" "$file" | head -n 1)
		[[ "$value" =~ ^[0-9]+$ ]] || {
			printf 'PSim metrics: cannot read %s from %s.\n' "$attribute" "$file" >&2
			exit 2
		}
		total=$((total + value))
		found=1
	done
	((found == 1)) || fail_missing "$SUREFIRE_DIR/TEST-*.xml"
	printf '%d\n' "$total"
}

count_pattern() {
	local pattern=$1
	local file=$2
	local count
	count=$(grep -c -- "$pattern" "$file" || true)
	printf '%d\n' "$count"
}

baseline_value() {
	local key=$1
	local value
	value=$(awk -F= -v key="$key" '$1 == key { print $2 }' "$BASELINE_FILE")
	[[ "$value" =~ ^[0-9]+$ ]] || {
		printf 'PSim metrics: baseline %s is missing or invalid in %s.\n' "$key" "$BASELINE_FILE" >&2
		exit 2
	}
	printf '%d\n' "$value"
}

display_value() {
	case "$1" in
		line_coverage_basis_points | branch_coverage_basis_points)
			printf '%d.%02d%%' "$((10#$2 / 100))" "$((10#$2 % 100))"
			;;
		*) printf '%d' "$2" ;;
	esac
}

is_higher_better() {
	case "$1" in
		line_coverage_basis_points | branch_coverage_basis_points | tests) return 0 ;;
		*) return 1 ;;
	esac
}

require_file "$JACOCO_CSV"
require_file "$CHECKSTYLE_XML"
require_file "$SPOTBUGS_XML"
require_file "$BASELINE_FILE"

coverage=$(awk -F, 'NR > 1 { lm += $8; lc += $9; bm += $6; bc += $7 }
	END {
		if (lc + lm == 0 || bc + bm == 0) exit 1
		printf "%.0f %.0f", 10000 * lc / (lc + lm), 10000 * bc / (bc + bm)
	}' "$JACOCO_CSV") || {
	printf 'PSim metrics: cannot calculate coverage from %s.\n' "$JACOCO_CSV" >&2
	exit 2
}
read -r line_coverage_basis_points branch_coverage_basis_points <<<"$coverage"
tests=$(read_attribute_total tests)
test_failures=$(read_attribute_total failures)
test_errors=$(read_attribute_total errors)
test_skipped=$(read_attribute_total skipped)
checkstyle_violations=$(count_pattern '<error ' "$CHECKSTYLE_XML")
spotbugs_findings=$(count_pattern '<BugInstance ' "$SPOTBUGS_XML")

mode=${1:---check}
case "$mode" in
	--check | --report | --ratchet) ;;
	-h | --help) print_help; exit 0 ;;
	*) print_help >&2; exit 2 ;;
esac

printf '%-34s %12s %12s %9s  %s\n' 'Metric' 'Current' 'Baseline' 'Delta' 'Gate'
printf '%-34s %12s %12s %9s  %s\n' '----------------------------------' '------------' '------------' '---------' '----'
regressions=0
for key in "${METRIC_KEYS[@]}"; do
	current=${!key}
	baseline=$(baseline_value "$key")
	delta=$((current - baseline))
	gate=PASS
	if is_higher_better "$key"; then
		if ((current < baseline)); then
			gate=REGRESSION
			regressions=$((regressions + 1))
		fi
	else
		if ((current > baseline)); then
			gate=REGRESSION
			regressions=$((regressions + 1))
		fi
	fi
	printf '%-34s %12s %12s %+9d  %s\n' \
		"$key" "$(display_value "$key" "$current")" "$(display_value "$key" "$baseline")" "$delta" "$gate"
done

if [[ "$mode" == --report ]]; then
	exit 0
fi
if ((regressions > 0)); then
	printf '\nPSim metrics gate failed with %d regression(s).\n' "$regressions" >&2
	printf 'Restore the metric or improve it beyond the committed baseline. Never lower the baseline.\n' >&2
	exit 1
fi

if [[ "$mode" == --ratchet ]]; then
	temporary=$(mktemp "${TMPDIR:-/tmp}/psim-metrics-baseline.XXXXXX") || exit 2
	{
		printf '# Monotonic PSim quality baseline. Update only with scripts/quality-metrics.sh --ratchet.\n'
		for key in "${METRIC_KEYS[@]}"; do
			printf '%s=%d\n' "$key" "${!key}"
		done
	} >"$temporary"
	mv -- "$temporary" "$BASELINE_FILE"
	printf '\nRatchet updated: %s\n' "$BASELINE_FILE"
else
	printf '\nPSim metrics gate passed.\n'
fi
