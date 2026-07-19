---
confidence: analyzed
related: [spec/adr/AADR-001-constrained-gherkin.md, spec/planning/Airlift-m2-freeze.md, spec/drafts/Airlift-IR.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/drafts/Airlift-IR.md, ir-spec/IR-SCHEMA.md]
trajectory: >
  Governs the M2 freeze scope: freeze the stack frame, mark the kind-set
  open, adding a kind = minor version. R4 probe at M3+ tests the envelope.
---

# AADR-005 — The IR is an open stack of typed claim-kinds; v1 populates one

**Tag:** stack load-bearing; current one-kind population incidental.
**Status:** active. **Origin:** reflexive D5 ("the most important honesty in this
document").

## Decision

The IR is designed as a *stack* of typed claim-kinds: behavior rules (Gherkin) —
**implemented**; domain schema (typed constraints) — partial (domain-model.yaml,
fixture vocabulary); invariants (property-based tests), processes (state machines /
model-based tests), contracts (OpenAPI/IDL) — **designed, not implemented**. What M2
freezes as v1.0 is the *stack frame* (claim-kind envelope, statuses, traceability,
glossary binding), with the kind-set explicitly open: adding a claim-kind is a minor
version, by design.

## Alternatives considered

* **Freeze "the IR" as the one kind the first seam needed** — the named danger. The
  tax seam happened to be expressible almost entirely as behavior rules + decision
  tables, so the spike never hit the walls that would have forced the other kinds.
  Canonizing that accident would confuse the sample with the population.
* **Implement all kinds up front** — rejected by D6's lesson: walls must be hit, not
  imagined; speculative generality is unsanctioned schema growth.

## Evidence

`spec/drafts/Airlift-IR.md` designed the stack before Goal 1; Goal 1's seam
exercised one kind end-to-end through E1–E4. The envelope (ID scheme, statuses,
traceability, glossary binding) proved kind-agnostic in use — nothing in the spine or
MCP design depends on the claim body being Gherkin specifically.

## Falsifier

A needed claim-kind cannot be added as a minor delta — i.e., the envelope turns out
to be over-fit to Gherkin (shared fields don't fit state machines or property
claims). This is the schema-level twin of AADR-001's falsifier: an inexpressible seam
first tests whether a new kind *can* be added cleanly; only if it can't does this
AADR reopen (major version territory).

## Falsifier observations

* 2026-07-19 — none tripped; only one kind has ever been populated, so the envelope's
  kind-agnosticism is asserted from design review, not exercised. R4's deliberate
  seam selection at M3+ is the scheduled probe.
