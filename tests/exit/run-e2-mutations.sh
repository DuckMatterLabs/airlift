#!/usr/bin/env bash
# Exit criterion E2: mutation testing.
# Apply each planted bug, run the blind suite, expect RED with the right claim named.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AIRLIFT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_DIR="$AIRLIFT_ROOT/tests/targets/ofbiz-tax"
source "$TARGET_DIR/target.env"
OUT_DIR="$AIRLIFT_ROOT/tests/out/$TARGET_NAME"
PY="$AIRLIFT_ROOT/.venv/bin/python"
MUT="$TARGET_DIR/mutations/mutations.py"
SUMMARY="$AIRLIFT_ROOT/tests/runs/e2-summary.txt"

: > "$SUMMARY"
caught=0
total=0
for mid in $($PY "$MUT" list | cut -d: -f1); do
  total=$((total + 1))
  echo "=== E2 mutation $mid ==="
  $PY "$MUT" apply "$mid"
  set +e
  "$TARGET_DIR/harness/run-tests.sh" airliftblind > "$AIRLIFT_ROOT/tests/runs/e2-$mid.log" 2>&1
  RUN_EXIT=$?
  $PY "$TARGET_DIR/harness/report-claims.py" \
      --results "$TEST_RESULTS_DIR/airliftblind.xml" \
      --tests-dir "$BLIND_TEST_DIR" --ir "$OUT_DIR/ir" > "$AIRLIFT_ROOT/tests/runs/e2-$mid-spine.txt" 2>&1
  set -e
  $PY "$MUT" revert
  if [ "$RUN_EXIT" -ne 0 ]; then
    caught=$((caught + 1))
    verdict="CAUGHT"
  else
    verdict="MISSED"
  fi
  claims="$(grep -E '^\s+✗' "$AIRLIFT_ROOT/tests/runs/e2-$mid-spine.txt" | sed 's/^ *✗ *//' | paste -sd ';' - || true)"
  echo "$mid: $verdict ${claims:+— violated: $claims}" | tee -a "$SUMMARY"
done

echo "=== E2 RESULT: $caught/$total mutations caught ===" | tee -a "$SUMMARY"
# "catch most" criterion: strictly more than half
[ $((caught * 2)) -gt "$total" ]
