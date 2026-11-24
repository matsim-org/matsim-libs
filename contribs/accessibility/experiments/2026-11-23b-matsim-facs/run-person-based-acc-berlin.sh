#!/bin/bash --login
#SBATCH --job-name=acc-submit
#SBATCH --output=acc-submit-%j.log
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --partition=ils
#SBATCH --cpus-per-task=12
#SBATCH --mem=64G
#SBATCH --time=10:00:00

set -euo pipefail

# Use facilities:<activity_type> for MATSim facilities, or a POI source such as spa.
opportunities=(
  facilities:dining
  facilities:edu_higher
  facilities:edu_kiga
  facilities:edu_other
  facilities:edu_primary
  facilities:edu_secondary
  facilities:leisure
  facilities:personal_business
  facilities:shop_daily
  facilities:shop_other
  facilities:work
  facilities:work_business
)
scenario_modes=(base:car base:pt base:teleportedWalk policy:car)

# SLURM executes a spooled copy, so submitted jobs use the submission directory.
if [[ -n "${SLURM_JOB_ID:-}" ]]; then
  cd -- "${SLURM_SUBMIT_DIR:?Missing SLURM submission directory}"
else
  cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
fi

input_root="10pct-v6.4"
base_dir="${BASE_INPUT_DIR:-${input_root}/berlin-autofrei-v6.4-baseCaseCtdExtended-10pct}"
policy_dir="${POLICY_INPUT_DIR:-${input_root}/berlin-autofrei-v6.4-policy-10pct-deparking-p20_d0_i0_max1000}"

shopt -s nullglob
jars=(*.jar)
if (( ${#jars[@]} != 1 )); then
  echo "Expected one JAR containing accessibility and its runtime dependencies in ${PWD}." >&2
  exit 1
fi
jar="${jars[0]}"

if (( $# == 0 )); then
  for input_dir in "$base_dir" "$policy_dir"; do
    if [[ ! -d "$input_dir" ]]; then
      echo "Simulation output folder not found: ${input_dir}" >&2
      exit 1
    fi
  done
  mkdir -p logs results
  result_root="$(mktemp -d "results/$(date +%Y%m%d-%H%M%S)-XXXXXX")"
  echo "Results: ${result_root}"
  script="./run-person-based-acc-berlin.sh"
  for opportunity in "${opportunities[@]}"; do
    poi="${opportunity#facilities:}"
    for scenario_mode in "${scenario_modes[@]}"; do
      scenario="${scenario_mode%:*}"
      mode="${scenario_mode#*:}"
      sbatch --job-name="acc-${scenario}-${poi}-${mode}" --output='logs/%x-%j.log' \
        "$script" "$result_root" "$opportunity" "$scenario" "$mode"
    done
  done
  exit 0
fi

if (( $# != 4 )); then
  echo "Run this launcher without arguments to submit the experiment." >&2
  exit 1
fi
result_root="$1"
opportunity="$2"
scenario="$3"
mode="$4"
case "$scenario" in
  base) input_dir="$base_dir" ;;
  policy) input_dir="$policy_dir" ;;
esac

opportunity_args=(--opportunity="$opportunity")
if [[ "$opportunity" == facilities:* ]]; then
  opportunity_args=(--opportunity=facilities --activity-type="${opportunity#facilities:}")
fi

sample="${SAMPLE:-0.1}"
output_dir="${result_root}/${scenario}"

module add java/25
echo "Scenario=${scenario} mode=${mode} opportunity=${opportunity} sample=${sample} input=${input_dir} output=${output_dir}"

java -Xms4g -Xmx52g -cp "$jar" \
  org.matsim.contrib.accessibility.run.RunPersonBasedAccBerlin \
  "${opportunity_args[@]}" \
  --scenario="$scenario" \
  --mode="$mode" \
  --measure-type=rawSum \
  --sample="$sample" \
  --scenario-dir="$input_dir" \
  --output-dir="$output_dir"
