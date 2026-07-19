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
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib.sh"

TARGET_DIR="$(cd "$1" && pwd)"
shift
source "$TARGET_DIR/target.env"

OUT_DIR="$AIRLIFT_ROOT/out/$TARGET_NAME"
IR_SPEC="$AIRLIFT_ROOT/ir-spec"
PY="$AIRLIFT_ROOT/.venv/bin/python"
mkdir -p "$OUT_DIR" "$RUNS_DIR/rendered"

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
    "DOMAIN_EXPERT_PERSONA=$DOMAIN_EXPERT_PERSONA" >&2
  echo "$out"
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
  local prompt; prompt="$(render "$template")"
  local attempt=0
  while true; do
    attempt=$((attempt + 1))
    echo "=== [$TARGET_NAME] stage=$stage attempt=$attempt ==="
    run_copilot "$TARGET_REPO" "$prompt" "${TARGET_NAME}-${stage}-a${attempt}" \
      --add-dir "$OUT_DIR" --add-dir "$IR_SPEC" --add-dir "$TARGET_DIR"
    if verrs="$("$PY" "$SCRIPT_DIR/validate.py" "$validator" "$OUT_DIR" --repo "$TARGET_REPO")"; then
      echo "$verrs"
      echo "=== stage $stage PASSED validation ==="
      return 0
    fi
    echo "$verrs"
    if [ "$attempt" -ge 3 ]; then
      echo "=== stage $stage FAILED after $attempt attempts ===" >&2
      return 1
    fi
    prompt="$RUNS_DIR/rendered/${TARGET_NAME}-${stage}-repair.md"
    {
      cat "$(render "$template")"
      printf '\n\n## REPAIR ROUND\nYour previous output failed deterministic validation. '
      printf 'Fix your existing output files in place (do not start over):\n\n```\n%s\n```\n' "$verrs"
    } > "$prompt"
  done
}

if [ "$1" = "all" ]; then
  set -- map testscape catalog backfill distill
fi
for stage in "$@"; do
  run_stage "$stage"
done
echo "=== pipeline complete: $* ==="
