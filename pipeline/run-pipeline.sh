#!/usr/bin/env bash
# Airlift distillation pipeline orchestrator (target-agnostic).
#
#   run-pipeline.sh <target-dir> <stage> [<stage>...]
#   stages: map testscape catalog backfill distill   (or: all)
#
# Each stage: render generic prompt with target descriptor -> run under the
# Copilot CLI + Opus 4.8 harness -> deterministic structural validation.
# On validation failure the stage gets up to 2 repair rounds (validator output
# is appended to a repair prompt), then the pipeline fails.
#
# Stage 4 (backfill) uses an external runner: the agent only writes tests; the
# pipeline compiles and executes the suite deterministically (E1-standard log
# capture) and stamps the run verdict into backfill-report.yaml. Compile errors
# or failing tests are fed back in repair rounds — sanctioned here because
# backfill is code-in-hand; blind-stage repair stays compile-errors-only.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib.sh"

[ $# -ge 2 ] || { echo "usage: run-pipeline.sh <target-dir> <stage>... | all" >&2; exit 2; }
TARGET_DIR="$(cd "$1" && pwd)"
shift
source "$TARGET_DIR/target.env"

OUT_DIR="$AIRLIFT_ROOT/tests/out/$TARGET_NAME"
IR_SPEC="$AIRLIFT_ROOT/ir-spec"
PY="$AIRLIFT_ROOT/.venv/bin/python"
mkdir -p "$OUT_DIR" "$RUNS_DIR/rendered"

SCHEMA_VERSION="$(tr -d '[:space:]' < "$IR_SPEC/VERSION")"
# Test seam: seed a mismatched version into the PROMPT only (validators still
# enforce $SCHEMA_VERSION) to drill the repair loop end-to-end deterministically.
PROMPT_SCHEMA_VERSION="${AIRLIFT_SEED_SCHEMA_VERSION:-$SCHEMA_VERSION}"

abs() { case "$1" in /*) echo "$1" ;; *) echo "$TARGET_DIR/$1" ;; esac; }
SEAM_FILE="$(abs "$SEAM_BRIEF_FILE")"
CONTRACT_FILE="$(abs "$HARNESS_CONTRACT")"

render() { # render <template-name> -> echoes rendered path
  local name="$1" out="$RUNS_DIR/rendered/${TARGET_NAME}-$1"
  "$PY" "$SCRIPT_DIR/render.py" "$SCRIPT_DIR/prompts/$name" "$out" \
    "SEAM_BRIEF=@$SEAM_FILE" \
    "OUT_DIR=$OUT_DIR" \
    "IR_SPEC=$IR_SPEC" \
    "HARNESS_CONTRACT=$CONTRACT_FILE" \
    "CLAIM_NAMESPACE=$CLAIM_NAMESPACE" \
    "DOMAIN_EXPERT_PERSONA=$DOMAIN_EXPERT_PERSONA" \
    "SCHEMA_VERSION=$PROMPT_SCHEMA_VERSION" >&2
  echo "$out"
}

external_backfill_run() { # compile + run the backfill suite, stamp run: into the report
  # stdout = human-readable verdict/failure detail (feeds repair rounds); logs to RUNS_DIR.
  local log="$RUNS_DIR/${TARGET_NAME}-backfill-run.log"
  if [ ! -f "$OUT_DIR/backfill-report.yaml" ]; then
    echo "backfill: agent did not write $OUT_DIR/backfill-report.yaml"
    return 1
  fi
  echo "[external runner] compile -> $log"
  if ! ( eval "$TARGET_COMPILE_CMD" ) >"$log" 2>&1; then
    echo "backfill: compile failed (external runner). Compiler output:"
    grep -E "error:|\.java:" "$log" | head -40
    return 1
  fi
  echo "[external runner] suite -> $log"
  rm -f "$BACKFILL_RESULTS_FILE"
  ( eval "$TARGET_BACKFILL_TEST_CMD" ) >>"$log" 2>&1 || true  # verdict comes from the results file
  if [ ! -f "$BACKFILL_RESULTS_FILE" ]; then
    echo "backfill: suite produced no results file at $BACKFILL_RESULTS_FILE (boot/infra failure; see $log)"
    return 1
  fi
  if ! "$PY" "$SCRIPT_DIR/stamp-backfill.py" "$OUT_DIR/backfill-report.yaml" \
       "$BACKFILL_RESULTS_FILE" "$TARGET_REPO" "$TARGET_BACKFILL_TEST_CMD"; then
    echo "backfill: suite red (external runner). Failing tests:"
    "$PY" "$SCRIPT_DIR/junitxml.py" "$BACKFILL_RESULTS_FILE" || true
    return 1
  fi
  "$PY" "$SCRIPT_DIR/junitxml.py" "$BACKFILL_RESULTS_FILE"
}

run_stage() { # run_stage <stage>
  local stage="$1" template validator
  case "$stage" in
    map)       template=stage1-map.md;       validator=map ;;
    testscape) template=stage2-testscape.md; validator=testscape ;;
    catalog)   template=stage3-catalog.md;   validator=catalog ;;
    backfill)  template=stage4-backfill.md;  validator=backfill ;;
    distill)   template=stage5-distill.md;   validator=ir ;;
    *) echo "unknown stage: $stage" >&2; return 2 ;;
  esac
  if [ "$stage" = backfill ]; then
    # fail fast BEFORE burning an agent run: the external runner needs these
    : "${TARGET_COMPILE_CMD:?target.env must define TARGET_COMPILE_CMD for the backfill stage}"
    : "${TARGET_BACKFILL_TEST_CMD:?target.env must define TARGET_BACKFILL_TEST_CMD for the backfill stage}"
    : "${BACKFILL_RESULTS_FILE:?target.env must define BACKFILL_RESULTS_FILE for the backfill stage}"
  fi
  local prompt; prompt="$(render "$template")"
  local attempt=0
  while true; do
    attempt=$((attempt + 1))
    echo "=== [$TARGET_NAME] stage=$stage attempt=$attempt ==="
    run_copilot "$TARGET_REPO" "$prompt" "${TARGET_NAME}-${stage}-a${attempt}" \
      --add-dir "$OUT_DIR" --add-dir "$IR_SPEC" --add-dir "$TARGET_DIR"
    local stage_errs="" stage_ok=1
    if [ "$stage" = backfill ]; then
      if ! stage_errs="$(external_backfill_run)"; then
        stage_ok=0
      fi
      echo "$stage_errs"
    fi
    if [ "$stage_ok" = 1 ]; then
      if stage_errs="$("$PY" "$SCRIPT_DIR/validate.py" "$validator" "$OUT_DIR" --repo "$TARGET_REPO")"; then
        echo "$stage_errs"
        echo "=== stage $stage PASSED validation ==="
        return 0
      fi
      echo "$stage_errs"
    fi
    if [ "$attempt" -ge 3 ]; then
      echo "=== stage $stage FAILED after $attempt attempts ===" >&2
      return 1
    fi
    prompt="$RUNS_DIR/rendered/${TARGET_NAME}-${stage}-repair.md"
    {
      cat "$(render "$template")"
      printf '\n\n## REPAIR ROUND\nYour previous output failed deterministic validation. '
      printf 'Fix your existing output files in place (do not start over):\n\n```\n%s\n```\n' "$stage_errs"
    } > "$prompt"
  done
}

if [ "$1" = "all" ]; then
  [ $# -eq 1 ] || { echo "\"all\" must be the sole stage argument" >&2; exit 2; }
  set -- map testscape catalog backfill distill
fi
for stage in "$@"; do
  run_stage "$stage"
done
echo "=== pipeline complete: $* ==="
