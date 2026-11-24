#!/bin/bash --login

#SBATCH --job-name=pbAcc_berlin_base          # name of your run, will be displayed in the joblist
#SBATCH --output=logs/pbAcc_base_%A_%a.log    # one log per task: <array job id>_<task index> (logs/ must exist before sbatch)
#SBATCH --array=0-8                             # 9 tasks in parallel: 3 POIs x 3 modes (see TASKS below)
#SBATCH --nodes=1                               # How many computing nodes do you need (for MATSim usually 1)
#SBATCH --ntasks=1                              # How many tasks should be run (For MATSim usually 1)
#SBATCH --partition=ils                         # partition
#SBATCH --cpus-per-task=12                      # Number of CPUs per task (For MATSim usually 8 - 12)
#SBATCH --mem=64G                              # RAM per task (Java heap -Xmx below must stay ~10G under this)
#SBATCH --time=5:00:00                         # Time limit per task hrs:min:sec (PT takes the longest)
#SBATCH --mail-type=END,FAIL                    # Send email on end and fail (once for the whole array)
#SBATCH --mail-user=lawton@vsp.tu-berlin.de     # Your email address

# ==========================================================
# Person-based accessibility, Berlin, scenario: BASE
#
# Every POI/mode combination runs as its own Slurm array task, so all 9 run
# at the same time instead of one after another.
#
# Usage (submit from this experiment folder):
#   mkdir -p logs
#   sbatch run-person-based-acc-berlin-base.sh               # all 9 combinations
#   sbatch --array=3-5 run-person-based-acc-berlin-base.sh   # only some tasks (indices below)
# Arguments starting with "--" are passed through to the Java class.
#
# Task index -> combination:
#   0 spa/car        1 spa/pt        2 spa/teleportedWalk
#   3 hospital/car   4 hospital/pt   5 hospital/teleportedWalk
#   6 jobcenter/car  7 jobcenter/pt  8 jobcenter/teleportedWalk
#
# Experiment folder layout:
#   run-person-based-acc-berlin-<scenario>.sh
#   RunPersonBasedAccBerlin-cluster.jar
#   logs/
#   <date>[a-z]-<scenario>/<poi>/      results, shared by all tasks of one submission
#
# Check progress / results:
#   squeue --me
#   sacct -j <array job id> --format=JobID,State,Elapsed,MaxRSS
# ==========================================================

SCENARIO="base"

# All inputs of this scenario: MATSim output files (network, plans, events,
# transit schedule; .zst, .gz or .xml) and the POI files.
INPUT_DIR="base"


# Share of persons to compute, applied when the plans are loaded.
# 1 = all persons. For a 10% scenario, 0.1 gives a 1%-sized run (~49,000 persons)
# while car travel times still come from the full 10% simulation.
# The runner uses seed 4711. Matching sample fractions and input population order reproduce the sample.
SAMPLE="1"

JAR="RunPersonBasedAccBerlin-cluster.jar"

POIS=(spa hospital jobcenter)
MODES=(car pt teleportedWalk)

# --- this task's combination ---
TASK_ID="${SLURM_ARRAY_TASK_ID:-0}"
ARRAY_ID="${SLURM_ARRAY_JOB_ID:-local$$}"
if (( TASK_ID >= ${#POIS[@]} * ${#MODES[@]} )); then
  echo "Task ${TASK_ID} has no combination (only $(( ${#POIS[@]} * ${#MODES[@]} )) defined)" >&2
  exit 1
fi
poi="${POIS[$(( TASK_ID / ${#MODES[@]} ))]}"
mode="${MODES[$(( TASK_ID % ${#MODES[@]} ))]}"

# --- output folder: <date>-<scenario>, then <date>a-<scenario>, ... ---
# All tasks of one submission must share the same folder. The first task to
# arrive creates it and writes the array job id into it; the other tasks find
# their folder by that id.
#
# To add tasks to an existing results folder (e.g. rerun one failed task), set
# OUTPUT_DIR when submitting:
#   OUTPUT_DIR=2026-11-18-<scenario> sbatch --array=6 <this script>
RUN_DATE="$(date +%F)"
if [[ -n "${OUTPUT_DIR:-}" ]]; then
  mkdir -p "${OUTPUT_DIR}"
fi
for suffix in "" {a..z}; do
  [[ -n "${OUTPUT_DIR:-}" ]] && break
  candidate="${RUN_DATE}${suffix}-${SCENARIO}"
  marker="${candidate}/.array_job_id"
  if mkdir "${candidate}" 2>/dev/null; then       # mkdir is atomic: exactly one task wins
    echo "${ARRAY_ID}" > "${marker}.tmp.$$" && mv "${marker}.tmp.$$" "${marker}"
    OUTPUT_DIR="${candidate}"
    break
  fi
  # folder exists: if it was just created, give its creator a moment to write the marker
  if [[ ! -f "${marker}" ]] && [[ -n "$(find "${candidate}" -maxdepth 0 -newermt '-2 minutes' 2>/dev/null)" ]]; then
    for _ in {1..30}; do [[ -f "${marker}" ]] && break; sleep 1; done
  fi
  if [[ "$(cat "${marker}" 2>/dev/null)" == "${ARRAY_ID}" ]]; then
    OUTPUT_DIR="${candidate}"
    break
  fi
done
if [[ -z "${OUTPUT_DIR}" ]]; then
  echo "Could not create or find an output folder for ${RUN_DATE}-${SCENARIO}" >&2
  exit 1
fi

module add java/25
java -version

echo "scenario: ${SCENARIO}"
echo "task:     ${ARRAY_ID}_${TASK_ID}"
echo "POI:      ${poi}"
echo "mode:     ${mode}"
echo "input:    ${INPUT_DIR}"
echo "sample:   ${SAMPLE} (seed 4711)"
echo "output:   ${OUTPUT_DIR}"
echo "=== $(date '+%F %T') opportunity=${poi} scenario=${SCENARIO} mode=${mode} ==="

java -Xms4g -Xmx52g -cp "${JAR}" \
  org.matsim.contrib.accessibility.run.RunPersonBasedAccBerlin \
  --opportunity="${poi}" \
  --scenario="${SCENARIO}" \
  --mode="${mode}" \
  --sample="${SAMPLE}" \
  --scenario-dir="${INPUT_DIR}" \
  --hospital-input-dir="${INPUT_DIR}" \
  --baeder-geojson="${INPUT_DIR}/baeder.geojson" \
  --output-dir="${OUTPUT_DIR}" \
  "$@"
status=$?

# Fix cluster permissions. Make the results accessible for everyone in our group
chmod -R 770 "${OUTPUT_DIR}" logs 2>/dev/null

if (( status != 0 )); then
  echo "FAILED: ${poi}/${mode} (exit code ${status})" >&2
  exit "${status}"
fi
echo "=== $(date '+%F %T') finished ${poi}/${mode} ==="
