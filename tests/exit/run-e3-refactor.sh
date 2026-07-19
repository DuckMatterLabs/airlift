#!/usr/bin/env bash
# Exit criterion E3: behavior-preserving refactor by a Copilot+Opus subagent;
# blind suite must stay green. The refactored file is preserved as an artifact and the
# working copy is restored afterwards.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AIRLIFT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$AIRLIFT_ROOT/pipeline/lib.sh"
TARGET_DIR="$AIRLIFT_ROOT/tests/targets/ofbiz-tax"
source "$TARGET_DIR/target.env"
OUT_DIR="$AIRLIFT_ROOT/tests/out/$TARGET_NAME"
PY="$AIRLIFT_ROOT/.venv/bin/python"

PROMPT="$RUNS_DIR/rendered/ofbiz-tax-refactor.md"
"$PY" "$AIRLIFT_ROOT/pipeline/render.py" "$AIRLIFT_ROOT/pipeline/prompts/refactor.md" "$PROMPT" \
  "SEAM_BRIEF=@$TARGET_DIR/seam.md" "OUT_DIR=$OUT_DIR"

echo "=== E3 refactor by Copilot+Opus ==="
run_copilot "$TARGET_REPO" "$PROMPT" "e3-refactor" --add-dir "$OUT_DIR"

echo "=== E3 compile + blind suite ==="
(cd "$TARGET_REPO" && ./gradlew compileJava compileTestJava -x checkstyleMain -x checkstyleTest --console=plain)
set +e
"$TARGET_DIR/harness/run-tests.sh" airliftblind
RUN_EXIT=$?
set -e
"$PY" "$TARGET_DIR/harness/report-claims.py" \
  --results "$TEST_RESULTS_DIR/airliftblind.xml" \
  --tests-dir "$BLIND_TEST_DIR" --ir "$OUT_DIR/ir" | tee "$AIRLIFT_ROOT/tests/runs/e3-spine.txt"

mkdir -p "$OUT_DIR/e3"
cp "$SEAM_PRIMARY_FILE" "$OUT_DIR/e3/TaxAuthorityServices.refactored.java"
(cd "$TARGET_REPO" && git diff -- "$SEAM_PRIMARY_FILE") > "$OUT_DIR/e3/refactor.diff" || true
git -C "$TARGET_REPO" checkout -- "$SEAM_PRIMARY_FILE"
(cd "$TARGET_REPO" && ./gradlew compileJava -x checkstyleMain --console=plain > /dev/null 2>&1) || true

if [ "$RUN_EXIT" -ne 0 ]; then
  echo "E3 RESULT: RED — refactor broke pinned behavior (see spine above)" >&2
  exit 1
fi
echo "E3 RESULT: GREEN — behavior-preserving refactor survived the blind suite."
