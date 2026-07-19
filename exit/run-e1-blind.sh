#!/usr/bin/env bash
# Exit criterion E1: blind test generation.
# A Copilot+Opus subagent sees ONLY the IR + harness contract (shell and web tools denied,
# file access restricted to a sandbox). Its tests are then installed and run against the
# real code. Repair loop: compile errors only — test failures are NEVER fed back.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AIRLIFT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
source "$AIRLIFT_ROOT/pipeline/lib.sh"
TARGET_DIR="$AIRLIFT_ROOT/targets/ofbiz-tax"
source "$TARGET_DIR/target.env"
OUT_DIR="$AIRLIFT_ROOT/out/$TARGET_NAME"

SANDBOX="$AIRLIFT_ROOT/runs/blind-sandbox"
rm -rf "$SANDBOX"
mkdir -p "$SANDBOX/generated"
cp -R "$OUT_DIR/ir" "$SANDBOX/ir"
cp "$TARGET_DIR/harness/harness-contract.md" "$SANDBOX/harness-contract.md"
# the IR's architecture stub is fine to keep: it is part of the IR deliverable

PROMPT="$AIRLIFT_ROOT/pipeline/prompts/blind-testgen.md"
GEN="$SANDBOX/generated/BlindTaxClaimTests.java"

attempt=0
while true; do
  attempt=$((attempt + 1))
  echo "=== E1 blind generation attempt $attempt (sandboxed, no shell, no web) ==="
  # --excluded-tools hides shell/web tools from the model entirely (verified: the agent
  # then has no shell and cannot read outside the sandbox even with --allow-all-tools).
  run_copilot "$SANDBOX" "$PROMPT" "e1-blind-a$attempt" \
    --excluded-tools shell web-fetch

  test -f "$GEN" || { echo "E1: agent produced no $GEN" >&2; exit 1; }
  cp "$GEN" "$BLIND_TEST_DIR/BlindTaxClaimTests.java"

  echo "=== E1 compile check ==="
  if COMPILE_LOG="$(cd "$TARGET_REPO" && ./gradlew compileTestJava -x checkstyleMain -x checkstyleTest --console=plain 2>&1)"; then
    echo "compile OK"
    break
  fi
  echo "$COMPILE_LOG" | grep -E "error:|\.java:" | head -40 || true
  if [ "$attempt" -ge 3 ]; then
    echo "E1: still not compiling after $attempt attempts" >&2
    exit 1
  fi
  # Repair round: compile errors only. No test output ever goes back to the agent.
  PROMPT="$SANDBOX/repair-prompt.md"
  {
    cat "$AIRLIFT_ROOT/pipeline/prompts/blind-testgen.md"
    printf '\n\n## REPAIR ROUND\nYour previous generated/BlindTaxClaimTests.java does not compile. '
    printf 'Fix compile errors ONLY; do not change any expected value. Compiler output:\n\n```\n%s\n```\n' \
      "$(echo "$COMPILE_LOG" | grep -E "error:|\.java:" | head -40)"
  } > "$PROMPT"
done

echo "=== E1 run against real code ==="
set +e
"$TARGET_DIR/harness/run-tests.sh" airliftblind
RUN_EXIT=$?
set -e
"$AIRLIFT_ROOT/.venv/bin/python" "$TARGET_DIR/harness/report-claims.py" \
  --results "$TEST_RESULTS_DIR/airliftblind.xml" \
  --tests-dir "$BLIND_TEST_DIR" --ir "$OUT_DIR/ir" | tee "$AIRLIFT_ROOT/runs/e1-spine.txt"
if [ "$RUN_EXIT" -ne 0 ]; then
  echo "E1 RESULT: RED — blind tests do not pass against real code (IR or testgen defect)" >&2
  exit 1
fi
echo "E1 RESULT: GREEN — blind, IR-only tests pass against the real code."
