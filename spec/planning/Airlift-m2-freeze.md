---
confidence: proposed
related: [spec/analysis/Airlift-reflexive.md, ir-spec/IR-SCHEMA.md, spec/planning/MILESTONES.md]
sources: [spec/planning/MILESTONES.md, spec/planning/Goal1-handoff.md, spec/analysis/Airlift-reflexive.md]
trajectory: >
  Next milestone up; unblocks M3-M7 (schema v1.0 + promotion tool are their
  fixed inputs); materializes spec/adr/.
---

# Airlift M2 spec — Freeze & harden the contract

Status: **COMPLETE** (executed 2026-07-19; evidence: `tests/runs/m2-freeze-report.md`).
Date: 2026-07-19.
Prerequisite: M1 complete (it is). Everything downstream (M3–M7) consumes this
milestone's outputs as fixed contracts.
Background: `MILESTONES.md` §M2, `spec/planning/Goal1-handoff.md` §6 (threads 4–6), `ir-spec/IR-SCHEMA.md`,
**`spec/analysis/Airlift-reflexive.md` — normative for this milestone**: the freeze is the moment
lock-in begins, so WHY-capture and escape hatches are part of the freeze, not follow-up.

## Goal summary

Turn the spike's living, renegotiable schema into a **versioned, shipped artifact** with
a chartered change process — and pay down the debt that would otherwise be silently
inherited by every later milestone. Three thrusts: freeze (schema v1.0 + self-ADRs),
spine seed (claim-status promotion tool), debt (determinism gaps in the pipeline).

The freeze's own framing (verbatim requirement, from the reflexive doc): **v1.0 is a
commitment device against silent drift, not a claim of final correctness.** The schema
version carries its own attestation status: v1.0 is `pinned` (survived one seam's full
proof stack), not `verified`.

## Workstream 1 — Schema v1.0

* `ir-spec/IR-SCHEMA.md` tagged **v1.0**; every IR manifest declares `schema_version`;
  every `pipeline/validate.py` stage checks against the declared version.
* **What is frozen**: the claim-kind *envelope* (ID scheme, status field, traceability
  binding, glossary resolution rule, tier semantics), statuses, file layout, constrained-
  Gherkin grammar for the behavior-rule kind. **What is explicitly open** (per reflexive
  D5): the claim-kind *set*. Adding a kind (state machine, property/invariant, contract)
  is a minor version by design — the schema text must say so.
* **Change process** (per reflexive §3): minor = additive, changelog entry + extended
  validators; major = breaking, requires a migration tool AND re-proof (migrated
  ofbiz-tax IR must pass a fresh-agent E1 rerun — migration sits on the truth path).
  A `CHANGELOG.md` in `ir-spec/` starts at v1.0.
* **Candidate first minor delta, decided (not deferred) during this milestone:**
  `fidelity: bug-compatible` claim field (reflexive AADR-010, IR-analysis pitfall 3) —
  the reference-equality quirk claim is the test case. Either ship it in v1.0 or record
  the AADR rationale for deferral.

## Workstream 2 — Self-ADR register (the reflexive requirement)

* Materialize `spec/analysis/Airlift-reflexive.md` §5 as `spec/adr/AADR-001.md … AADR-011.md`
  (decision, alternatives considered, evidence, tag load-bearing/incidental,
  **falsifier**, status).
* The delta process gains a checklist item: any change touching a load-bearing decision
  updates or adds an AADR at merge time. This is cheap now and impossible to reconstruct
  later — the entire point of doing it at freeze time.

## Workstream 3 — Claim-status promotion tool (the M7 spine's seed)

A small deterministic tool (`pipeline/promote.py`), no LLM:

* **Inputs**: JUnit XML test results (`runtime/logs/test-results/*.xml`), the spine
  report (`report-claims.py` output), the IR directory.
* **Rules**: claim bound to a green backfill test ⇒ at least `pinned`; claim bound to a
  green blind test ⇒ `verified`; conflicts (red anywhere) ⇒ no promotion, report why.
  Records an attestation on the claim: `{tier, evidence: <results file>, sha, date}`.
* **Idempotent**; re-running on the same inputs is a no-op. Dry-run mode prints the
  diff. Statuses only ever move along `extracted → pinned → verified` here (demotion is
  M7's job — do not build it yet, but leave the attestation record shaped so demotion
  can read it).

## Workstream 4 — Debt paydown

* **Stage-4 external-runner variant**: backfill test execution driven by the runner (as
  E1 does) instead of the agent invoking gradle itself — determinism and log capture
  match the E1 standard.
* **Repair-loop exercise**: run `run-pipeline.sh` distill stage end-to-end through its
  own ≤3-attempt repair loop (it was exercised manually during the spike after an
  interrupted run). Seed a deliberate validator failure if needed to prove the loop.
* **Commit the repo.** The airlift repo is uncommitted; committing requires the user's
  explicit go-ahead (their repo — ask, don't assume). The OFBiz-side `airlift` branch is
  already committed. Proposed shape: one initial commit for the proven M1 state, then
  conventional commits per milestone workstream.

## Deliverables

* `ir-spec/IR-SCHEMA.md` v1.0 + `ir-spec/CHANGELOG.md`; version-checking validators.
* `spec/adr/` register (11 seed AADRs).
* `pipeline/promote.py` + tests.
* Stage-4 external runner; repair-loop run log for the distill stage.
* Committed repo (subject to user go-ahead).

## Exit criteria

1. Full pipeline rerun on ofbiz-tax validates clean against tagged v1.0 (every stage,
   version-checked).
2. `promote.py` run on the existing E1/E2 reports yields **35 `verified` claims with
   zero manual edits**; second run is a no-op.
   *[Corrected at execution, 2026-07-19: "35" conflated the blind test-method count
   with the claim count — the IR has 34 claims, of which the blind suite binds 27
   distinct (35 methods). Actual, correct result: 27 `verified` + 3 `pinned`
   (backfill-bound only: the TAX.DISPLAY claims) + 4 `extracted` (no bound test in
   either suite), zero manual edits, second run a byte-level no-op.]*
3. All 11 seed AADRs exist and every load-bearing schema element traces to one.
4. Distill-stage repair loop demonstrated end-to-end through `run-pipeline.sh` itself.
5. The `fidelity:` decision is made and recorded (shipped or AADR-deferred).

## Non-goals

* No new claim-kinds, no second target, no demotion/staleness (M3, M7).
* No schema changes motivated by speculative generality — walls must be hit (reflexive
  D6); M3 is where walls live.
