---
confidence: analyzed
related: [spec/adr/AADR-003-atom-per-file-graph-as-view.md, spec/adr/AADR-011-junit3-spine-binding.md, ir-spec/IR-SCHEMA.md]
sources: [ir-spec/IR-SCHEMA.md, tests/out/ofbiz-tax/RUN-REPORT.md, spec/planning/Goal1-handoff.md]
trajectory: >
  Records the identity scheme frozen at v1.0 (IR-SCHEMA sections 2 and 7).
  Added at the M2 freeze when the section-14 tracing pass showed these frozen
  elements had no owning AADR (register rule 1).
---

# AADR-012 — Stable claim IDs; fragment identity = symbol + role, lines are evidence

**Tag:** load-bearing. **Status:** active.
**Origin:** M2 freeze tracing pass (IR-SCHEMA.md §14 required every frozen element to
trace to an AADR; the identity scheme of §2/§7 had none — recorded here rather than
frozen silently).

## Decision

Two identity rules, frozen at v1.0:

1. **Claim IDs are `<DOMAIN>.<AREA>.<SLUG>`, stable forever.** Renaming is prohibited;
   a claim whose meaning changes is superseded (`superseded_by`), never renamed. IDs
   are the join key for everything downstream: test bindings (`@AirliftClaim`),
   attestations, the spine, provenance, the future Ledger.
2. **Fragment identity is symbol + role; line ranges are evidence at the
   `source_binding.commit`, not identity.** Validators that check line coverage do so
   against the file content *at the declared commit* (enforced by `validate.py` since
   M2), never against a later working tree.

## Alternatives considered

* **Line-range identity** — rejected: any edit above a fragment invalidates every
  binding below it; E3's 675-line refactor would have orphaned most of the map.
* **Content-hash identity** — rejected for v1: a hash changes on every touch including
  formatting, which makes identity *more* brittle than symbols for the entanglement
  pathology; revisit if a target's symbols are unstable (generated code).
* **Renamable IDs with a rename log** — rejected: a rename log is a second source of
  truth that every consumer must replay; supersession keeps the join key immutable and
  makes deprecation explicit.

## Evidence

E3 (behavior-preserving refactor, 675-line diff, 10 extracted methods): the blind
suite and spine stayed green and correctly bound — possible only because bindings are
claim-ID- and symbol-based, not line-based. The M2 baseline probe made the
evidence-not-identity rule operational: stage 5's comment-only annotations shifted the
primary file by 30 lines and the old working-tree line check failed spuriously;
validating at the declared commit fixed it without touching the map.

## Falsifier

* A real semantic evolution that supersession chains cannot express cleanly (e.g. a
  claim splitting into N claims that consumers must treat as continuous history) —
  would force a richer identity model (major version).
* A second target whose fragment symbols are unstable across commits (generated or
  heavily macro-expanded code) — would force content-anchored fragment identity.

## Falsifier observations

* 2026-07-19 — none; single target, supersession unexercised (no claim superseded yet).
