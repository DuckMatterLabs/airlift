#!/usr/bin/env bash
# Exit criterion E4: one deliberately wrong change — flip the tax-exemption rule.
# The blind suite must go red AND the spine must name the violated exemption claim.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AIRLIFT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_DIR="$AIRLIFT_ROOT/tests/targets/ofbiz-tax"
source "$TARGET_DIR/target.env"
OUT_DIR="$AIRLIFT_ROOT/tests/out/$TARGET_NAME"
PY="$AIRLIFT_ROOT/.venv/bin/python"
MUT="$TARGET_DIR/mutations/mutations.py"

echo "=== E4: flipping the tax-exemption rule ==="
"$PY" "$MUT" apply M1-exempt-flip
set +e
"$TARGET_DIR/harness/run-tests.sh" airliftblind > "$AIRLIFT_ROOT/tests/runs/e4.log" 2>&1
RUN_EXIT=$?
"$PY" "$TARGET_DIR/harness/report-claims.py" \
  --results "$TEST_RESULTS_DIR/airliftblind.xml" \
  --tests-dir "$BLIND_TEST_DIR" --ir "$OUT_DIR/ir" | tee "$AIRLIFT_ROOT/tests/runs/e4-spine.txt"
SPINE_EXIT=$?
set -e
"$PY" "$MUT" revert

if [ "$RUN_EXIT" -eq 0 ]; then
  echo "E4 RESULT: FAILED — flipped exemption rule was NOT caught" >&2
  exit 1
fi
if ! grep -q "✗ TAX\.EXEMPT" "$AIRLIFT_ROOT/tests/runs/e4-spine.txt"; then
  echo "E4 RESULT: PARTIAL — suite went red but the spine did not name a TAX.EXEMPT claim" >&2
  exit 1
fi
echo "E4 RESULT: PASSED — bad change caught and the violated exemption claim named:"
grep -E '^\s+✗' "$AIRLIFT_ROOT/tests/runs/e4-spine.txt"
