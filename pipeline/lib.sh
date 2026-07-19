#!/usr/bin/env bash
# Airlift pipeline library: Copilot CLI (BYOK: Anthropic / Opus 4.8) invocation helpers.
# Every distillation stage and every exit-criteria agent runs through run_copilot().
set -euo pipefail

AIRLIFT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNS_DIR="$AIRLIFT_ROOT/tests/runs"

airlift_env() {
  # Anthropic key from .env; Copilot BYOK pointed at Anthropic with Opus 4.8.
  set -a
  # shellcheck disable=SC1091
  source "$AIRLIFT_ROOT/.env"
  set +a
  export COPILOT_PROVIDER_TYPE=anthropic
  export COPILOT_PROVIDER_BASE_URL=https://api.anthropic.com
  export COPILOT_PROVIDER_API_KEY="$ANTHROPIC_API_KEY"
  export COPILOT_MODEL=claude-opus-4-8
  export COPILOT_PROVIDER_MAX_OUTPUT_TOKENS=16000
}

# run_copilot <workdir> <prompt-file> <logname> [extra copilot args...]
# Non-interactive, all tools allowed inside <workdir>; extra dirs via --add-dir args.
run_copilot() {
  local workdir="$1" prompt_file="$2" logname="$3"
  shift 3
  airlift_env
  mkdir -p "$RUNS_DIR"
  local log="$RUNS_DIR/${logname}.log"
  echo "[airlift] copilot stage=$logname workdir=$workdir model=$COPILOT_MODEL" | tee -a "$log"
  # --no-custom-instructions: stages must be driven by our prompt only, not stray AGENTS.md
  # --disable-builtin-mcps: no GitHub MCP; the pipeline is local-only
  copilot -p "$(cat "$prompt_file")" \
    -C "$workdir" \
    --allow-all-tools \
    --no-ask-user \
    --no-custom-instructions \
    --disable-builtin-mcps \
    --no-auto-update \
    --log-level error \
    --log-dir "$RUNS_DIR/copilot-logs" \
    "$@" 2>&1 | tee -a "$log"
}
