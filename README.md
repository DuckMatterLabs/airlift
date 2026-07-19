# Airlift — business-logic distillation pipeline

Airlift distills business behavior out of entangled legacy code into a typed, **provable**
IR: a subagent that has never seen the source must be able to generate tests from the IR
alone, and those tests must run green against the live code (and red against mutated code).
Design papers live in `spec/`: `spec/planning/` (milestones + goal specs, incl.
`MILESTONES.md`), `spec/analysis/` (critical analyses + the reflexive self-ADR doc),
`spec/drafts/` (the original prose: whitepaper, IR, distillation discussions).

This repository contains three separable things:

1. the **Airlift IR schema** — target-agnostic;
2. the **distillation pipeline** — target-agnostic, executed by GitHub Copilot CLI
   (`copilot -p`) with Anthropic **Opus 4.8** via BYOK (key in `.env`);
3. the first **target plugin** (`tests/targets/ofbiz-tax/`) — Apache OFBiz sales-tax seam —
   plus the completed proof run against it.

> Continuing this work in a new session? Start at **`CLAUDE.md`** (constitution) →
> `spec/index.md` (map) → `spec/log.md` (session memory) → **`spec/planning/Goal1-handoff.md`**.
> Full narrative of the completed proof run: **`tests/out/ofbiz-tax/RUN-REPORT.md`**.

## Components

### `ir-spec/IR-SCHEMA.md` — the IR (normative)

Typed claim atoms (`behavior | invariant | domain | contract | config`), one YAML file per
claim: constrained Gherkin bodies over a mandatory glossary, worked numeric scenarios,
decision tables, stable claim IDs and fragment IDs, `traceability.yaml` binding claims to
source fragments, per-claim provenance stubs (PR-history), and an architecture stub
(ADRs + mustache-parameterized pattern library) kept deliberately outside the claims —
the same claims must survive an architecture swap.

### `pipeline/` — the engine (target-agnostic)

| File | Role |
|---|---|
| `prompts/stage1-map.md` | Map: inventory + classify fragments (business-logic / plumbing / boilerplate / **fused**), total line coverage of the seam's primary file |
| `prompts/stage2-testscape.md` | Test landscape: what existing tests actually pin (evidence-based, auditable search log) |
| `prompts/stage3-catalog.md` | Catalog: distinct business behaviors with observability + priority |
| `prompts/stage4-backfill.md` | Backfill: code-in-hand tests pin every bare direct behavior before distilling |
| `prompts/stage5-distill.md` | Distill: emit the full IR + in-source fragment annotations + provenance + architecture stub |
| `prompts/blind-testgen.md` | E1 agent prompt: write tests from IR + harness contract only |
| `prompts/refactor.md` | E3 agent prompt: behavior-preserving refactor of the seam |
| `lib.sh` | `run_copilot`: Copilot CLI invocation with Anthropic BYOK (`COPILOT_MODEL=claude-opus-4-8`), tool/path policy, logging |
| `render.py` | Substitutes `{{PLACEHOLDER}}`s from a target descriptor; fails on unresolved placeholders |
| `validate.py` | Deterministic structural validators per stage (YAML shape, cross-references, fragment line-coverage, glossary closure, scenario presence on core claims) |
| `run-pipeline.sh` | Orchestrator: render → run under Copilot → validate; ≤3 attempts per stage with validator output fed back as a repair prompt |

Prompts contain **zero** target specifics. A target supplies them via its descriptor.

### `tests/targets/ofbiz-tax/` — the first target plugin

| Piece | Role |
|---|---|
| `target.env` | Descriptor: repo path, claim namespace, domain persona, test/compile commands, output locations |
| `seam.md` | Seam brief injected into the generic prompts (entry points, contract files, schema files, provenance-critical behaviors) |
| `harness/src/...` | Test harness installed into OFBiz: `TaxFixture` (fixture DSL hiding all framework plumbing), `AirliftTaxTestCase` (base + exact-decimal asserts), `TaxCalcResult` (architecture-free result view), `AirliftClaim` (test↔claim binding), `SmokeTaxTests` (harness self-test) |
| `harness/harness-contract.md` | The ONLY architecture the blind agent sees: test mechanics, fixture verbs, worked example — **no behavior semantics** |
| `harness/install.sh` / `run-tests.sh` | Idempotent installer; suite runner (`airliftsmoke` / `airliftbackfill` / `airliftblind`) with JUnit-XML verdict parsing |
| `harness/report-claims.py` | The **verification spine**: failing test → `@AirliftClaim` → claim ID + title ("which claim was violated") |
| `mutations/mutations.py` | 7 planted, realistic bugs (exemption flip, threshold boundary `≤`→`<`, base-drop, shipping-drop, rollup direction, inheritance cut, audit-trail zeroing); apply/revert against the committed git baseline |

### `tests/exit/` — the proof runners

* `run-e1-blind.sh` — sandboxed Copilot agent (no shell, no web, no repo file access —
  empirically verified) writes tests from `ir/` + contract; compile-error-only repair loop
  (never test failures — that would leak behavior); suite adjudicated externally.
* `run-e2-mutations.sh` — apply each bug → run blind suite → spine report → revert;
  passes when >half are caught.
* `run-e3-refactor.sh` — Copilot refactors the seam behavior-preservingly; blind suite must
  stay green; refactored file + diff archived to `tests/out/ofbiz-tax/e3/`, working copy restored.
* `run-e4-flip.sh` — flips the tax-exemption rule; passes only if the suite goes red AND
  the spine names a `TAX.EXEMPT` claim.

### `tests/out/ofbiz-tax/` — products of the completed run

`fragment-map.yaml` (79 fragments, 20 fused seams), `coverage-gaps.yaml` (zero native tests
touch the seam), `behavior-catalog.yaml` (30 behaviors / 7 areas), `backfill-report.yaml`
(15 behaviors pinned, 19 tests green), `ir/` (**34 claims, 75 glossary terms**, domain model
with fixture vocabulary, traceability, provenance, architecture stub), `e3/` (refactor
artifacts), `RUN-REPORT.md`.

In the OFBiz clone (branch `airlift`): the installed harness, the 19-test backfill suite,
the 35-test blind suite, and 30 comment-only `// AIRLIFT: F-TAS-NNN` annotations in
`TaxAuthorityServices.java`.

## Results of the proof run (2026-07-19)

| Exit criterion | Verdict |
|---|---|
| E1 — blind IR-only tests green vs real code | **PASSED** 35/35 |
| E2 — planted bugs caught, claims named | **PASSED** 7/7 |
| E3 — behavior-preserving refactor stays green | **PASSED** 35/35 |
| E4 — flipped exemption rule caught and named | **PASSED** (4 TAX.EXEMPT claims named) |

The first blind trial (30/35) exposed 3 claim ambiguities and 1 harness bug; per the method,
the **claims** were repaired (to match the code, never the tests) and a fresh blind agent
then reached 35/35 — the blind exit criterion acting as the forcing function that makes the
IR earn its shape.

## Running

```bash
./pipeline/run-pipeline.sh tests/targets/ofbiz-tax all      # stages: map testscape catalog backfill distill
./tests/exit/run-e1-blind.sh && ./tests/exit/run-e2-mutations.sh && ./tests/exit/run-e4-flip.sh && ./tests/exit/run-e3-refactor.sh
```

Prerequisites, one-time OFBiz setup, and operational gotchas: see `spec/planning/Goal1-handoff.md` §3–4.

## Adding a target

Create `tests/targets/<name>/` with `target.env`, `seam.md`, a harness honoring the
harness-contract pattern (fixture DSL + claim binding + suite runner + claim reporter), and
a mutation kit. No pipeline changes required.
