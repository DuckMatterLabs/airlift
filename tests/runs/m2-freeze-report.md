---
confidence: proven
related: [spec/planning/Airlift-m2-freeze.md, ir-spec/IR-SCHEMA.md, ir-spec/CHANGELOG.md, spec/adr/README.md]
sources: [tests/runs/m2-promotion/, tests/runs/m2-repair-drill.log, tests/runs/m2-ext-runner-demo.log, spec/planning/Airlift-m2-freeze.md]
trajectory: >
  Evidence record for the M2 freeze execution (2026-07-19). Referenced by
  MILESTONES.md and spec/log.md; stays with its run artifacts forever.
---

# M2 execution report — freeze & harden the contract

Executed 2026-07-19 (single session). Spec: `spec/planning/Airlift-m2-freeze.md`.
All five exit criteria met; two spec corrections recorded below (not silently edited).

## Exit criteria

| # | Criterion | Result |
|---|---|---|
| 1 | Full pipeline validation clean against tagged v1.0, every stage version-checked | **PASS** — all five validators OK on `tests/out/ofbiz-tax` (map/testscape/catalog/backfill/ir), each checking `schema_version` against `ir-spec/VERSION` |
| 2 | `promote.py` on E1/E2 evidence: verified claims, zero manual edits, second run no-op | **PASS** — 96 deterministic changes: **27 claims → `verified`**, 3 stay `pinned`, 4 stay `extracted`; 42 attestations recorded; second run byte-level no-op (`tests/runs/m2-promotion/promote-*.txt`) |
| 3 | All seed AADRs exist; every load-bearing schema element traces to one | **PASS** — AADR-001…011 pre-existed; tracing pass (IR-SCHEMA §14) exposed the frozen identity scheme as unrecorded → **AADR-012** added |
| 4 | Distill repair loop demonstrated end-to-end through `run-pipeline.sh` | **PASS** — 3 attempts: a1 organic structural failure (traceability `claims` emitted as dict; validator TypeError fed back verbatim), a2 the seeded `schema_version: 0.9` rejection, a3 repaired → `[validate:ir] OK` (`tests/runs/m2-repair-drill.log`, output archived at `tests/runs/m2-repair-drill-out/`) |
| 5 | `fidelity:` decision made and recorded | **PASS** — **deferred from v1.0**; rationale in `spec/adr/AADR-010-bug-compatible-fidelity.md` §M2 decision; on record in `ir-spec/CHANGELOG.md` as the v1.1 candidate |

## Corrections to the spec (recorded, not fudged)

1. **Exit criterion 2 said "35 verified claims"** — arithmetically impossible: 35 is the
   blind test-*method* count; the IR has 34 claims, the blind suite binds 27 distinct.
   True result: 27 `verified` + 3 `pinned` (TAX.DISPLAY.\*, backfill-bound only) +
   4 `extracted` (TAX.CONTRACT.\*, TAX.ORDER.VALUE-WEIGHT, TAX.VAT.PRICE-GROUP-SCOPE —
   honest gaps, no bound test in either suite). Correction noted in the spec file.
2. **"Full pipeline rerun"** (criterion 1) was read as: rerun the five deterministic
   validators against the existing proven artifacts under v1.0 — not regenerate the IR
   with fresh LLM runs, which would have discarded the E1–E4-proven artifacts that
   exit 2's "existing E1/E2 reports" explicitly depends on. The live pipeline path was
   exercised separately (criterion 4's distill drill; stage-4 runner demo below).

## Deliverables shipped

* `ir-spec/IR-SCHEMA.md` **v1.0** (frontmatter; §0 version declaration; §11 change
  process; §12 statuses/attestations/promotion; §13 frozen-vs-open; §14 AADR
  traceability) + `ir-spec/VERSION` + `ir-spec/CHANGELOG.md` (v1.0 entry, observability
  erratum, deferred-delta record).
* Version-checking validators: every stage artifact must declare `schema_version`;
  at-commit line-coverage validation (`git show`, byte-level count, **no working-tree
  fallback** when a commit is declared); attestation/status integrity checks
  (`pinned`/`verified` require a supporting attestation; manifest ↔ claim-file
  status consistency); observability enum enforced on claims.
* `pipeline/promote.py` + `pipeline/junitxml.py` + `pipeline/stamp-backfill.py` +
  29 unit tests (`pipeline/test_*.py`). Target side: `report-claims.py --spine-out`
  emits the machine-readable spine (IR-SCHEMA §8); binding extraction stays in the
  target plugin (AADR-011 observation recorded).
* Stage-4 **external runner** in `run-pipeline.sh` (agent writes tests; the pipeline
  compiles, runs the suite, stamps `run:` deterministically, feeds failures back —
  sanctioned for the code-in-hand stage). Demonstrated green with the real function
  body against the real committed suite: `tests/runs/m2-ext-runner-demo.log`
  (19/19, sha-stamped, validator OK). A full agent-driven stage-4 rerun was
  deliberately NOT performed — it would overwrite the proven M1 backfill suite for a
  mechanism test; the agent-facing half (repair rounds) is the same machinery the
  distill drill exercised. First natural full exercise: M3's new target.
* AADR register updates: AADR-006 expired (freeze executed), AADR-010 (fidelity
  deferral decision), AADR-011 (spine mechanism/principle split observation),
  **AADR-012 new** (stable claim IDs; fragment lines = evidence-at-commit).
* Promotion evidence chain, archived with the run: fresh green suite runs
  (backfill 19/19, blind 35/35, 2026-07-19, sha `1a7d91a2`), JUnit XMLs + spine
  files + dry-run/apply/no-op transcripts in `tests/runs/m2-promotion/`. Claim
  attestations reference the archived repo-relative XML paths.

## Adversarial review before the truth path moved

A 30-agent review workflow (4 dimensions → per-finding adversarial verification) ran
over the freeze changes before promotion or the drill executed: 26 raw findings,
**18 confirmed, 8 refuted** — all 18 fixed. Notable catches: dry-run omitted the
manifest edits a real run performs; `<skipped>` JUnit cases counted as green;
`git show` failures silently fell back to the working tree (contradicting the schema's
own at-commit rule); a target-specific `OFBIZ_ROOT` in `pipeline/lib.sh`
(target-agnostic invariant violation, removed); YAML-injection risks in the spine
writer and run-stamper (now JSON-escaped); the schema/validator observability-enum
mismatch (v1.0 erratum).

## State after M2

Statuses now carry evidence: 27 `verified` / 3 `pinned` / 4 `extracted`, every
non-extracted status backed by sha-stamped attestations. The delta regime is in force:
schema changes land as versioned changelog entries (minor additive / major with
migration + fresh-agent E1 re-proof), load-bearing changes update the AADR register at
merge. v1.0's own attestation status is `pinned`, not `verified` — M3 (second target,
cross-model E1, mutation sweep) is what buys `verified`.

OFBiz working tree confirmed clean after all runs (branch `airlift`, HEAD `1a7d91a2`).
