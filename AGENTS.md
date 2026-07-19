# Airlift — agent entry point

You are working on Airlift. The constitution is **`CLAUDE.md`** in this directory —
named for the harness that authored M1/M2, but its content is harness-agnostic and
binds every agent working on this repo. Read it first, in full, then follow its
orientation order: `spec/index.md` → `spec/log.md` (newest entry) → the latest
`spec/planning/Goal<N>-handoff.md` (its §8 is the Copilot takeover briefing) → the
spec of whatever milestone is next.

Hard rules you will be tempted to break (rationale in the constitution and
`spec/adr/`):

* Nothing target-specific in `pipeline/` or `ir-spec/`; targets live under
  `tests/targets/<name>/`.
* Schema changes only as versioned deltas (`ir-spec/CHANGELOG.md` + `VERSION` bump);
  validators are extended, never weakened; claim statuses move only via
  `pipeline/promote.py`.
* Blind test generation never sees source; blind repair feedback = compile errors only.
* A red blind test means the IR (or harness) is wrong — fix that, never the test, then
  rerun with a fresh agent.
* Commit only when the user asks. Append a `spec/log.md` entry before ending any
  session that changed anything.

Note: pipeline stage agents run with `--no-custom-instructions` and are driven only by
their rendered prompts — this file exists for interactive development sessions.
