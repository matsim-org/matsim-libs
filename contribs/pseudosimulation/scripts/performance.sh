#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(git -C "$MODULE_DIR" rev-parse --show-toplevel 2>/dev/null) || {
	printf 'PSim performance: cannot locate the Git repository.\n' >&2
	exit 2
}

PLANS=${PSIM_BENCHMARK_PLANS:-20000}
WARMUPS=${PSIM_BENCHMARK_WARMUPS:-3}
ROUNDS=${PSIM_BENCHMARK_ROUNDS:-7}
JVM_ARGUMENTS=${PSIM_BENCHMARK_JVM_ARGS:-}

if [[ "${1:-}" == "--jfr" ]]; then
	mkdir -p "$MODULE_DIR/target"
	JVM_ARGUMENTS+=" -XX:StartFlightRecording=filename=$MODULE_DIR/target/psim-performance.jfr,settings=profile,dumponexit=true"
elif [[ -n "${1:-}" ]]; then
	printf 'Usage: performance.sh [--jfr]\n' >&2
	exit 2
fi

exec mvn --batch-mode -f "$REPO_ROOT/pom.xml" \
	-pl contribs/pseudosimulation -am \
	-Dtest=PSimPerformanceBenchmark \
	-Dsurefire.failIfNoSpecifiedTests=false \
	-Dmatsim.preferLocalDtds=true \
	-Dmaven.test.redirectTestOutputToFile=false \
	-Dpsim.benchmark.plans="$PLANS" \
	-Dpsim.benchmark.warmups="$WARMUPS" \
	-Dpsim.benchmark.rounds="$ROUNDS" \
	-DargLine="$JVM_ARGUMENTS" \
	test
