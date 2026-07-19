---
confidence: analyzed
related: [spec/adr/AADR-003-atom-per-file-graph-as-view.md, spec/planning/Airlift-m2-freeze.md, Code-Digital-Twin]
sources: [spec/analysis/Airlift-reflexive.md, spec/analysis/IR-analysis.md, spec/planning/Goal1-handoff.md]
trajectory: >
  DECIDED at M2 (2026-07-19): deferred from v1.0; remains the candidate first
  minor delta (v1.1), recorded in ir-spec/CHANGELOG.md. CDT's constrained-by
  edge is the queryable long-term home once the graph view exists.
---

# AADR-010 — Bug-compatible extraction without a queryable fidelity marker

**Tag:** incidental — known debt. **Status:** active — delta candidate,
**deferred from v1.0 at the M2 freeze** (decision below).
**Origin:** reflexive register; IR-analysis pitfall 3.

## Decision

Extraction preserves bug-compatible behavior as claims ("bug-compatible behavior is
the requirement"), and v1 has **no queryable marking** distinguishing "intended rule"
from "accident we're preserving." The `notes:` field can hold the observation but is
not queryable.

## Alternatives considered

* **"Fix" the quirk during extraction** — rejected: destroys migration fidelity; the
  blind suite must reproduce the system that exists, not the system someone wishes
  existed.
* **Add a `fidelity:` field in v1** — deferred at spike time (schema was still
  co-evolving); now the candidate first minor delta.

## Evidence

The concrete instance: `getTaxAdjustments` (~line 499) compares
`itemQuantity != BigDecimal.ZERO` by REFERENCE — deliberately preserved, flagged in
Goal1-handoff §4 as "don't fix it." Right for migration fidelity, but the IR now
enshrines a bug as a claim indistinguishable from a rule: the distillate can launder
defects into requirements. Reflexively, this is also the lens for the register
itself — which of our *design* choices are accidents preserved because tests now pass
against them? (That's what the incidental tag exists to mark.)

## Falsifier

This AADR records a debt, so its trigger is a *consumer*, not a contradiction: the
first real consumer that needs to distinguish rule from accident (a migration
deciding what to carry forward; an Airbase intent diff asking "is this behavior
wanted?") forces the delta. Design options on record: a `fidelity: bug-compatible`
claim field (cheap, v1.x), or — per the CDT borrowing in IR-analysis — a constraint
node with a `constrained-by` edge, which makes fidelity queryable through the graph
view rather than per-file grep.

## Falsifier observations

* 2026-07-19 — no consumer has forced it yet. Log entry (b) records the M2 decision
  point: ship `fidelity:` in v1.0 or AADR-defer it.

## M2 decision (2026-07-19): deferred from v1.0

The M2 freeze spec required this decided, not postponed. Decision: **do not ship
`fidelity:` in v1.0**; it stays the candidate first minor delta (v1.1), on record in
`ir-spec/CHANGELOG.md`. Rationale:

1. **The AADR's own falsifier discipline.** The trigger is "first real consumer that
   needs to distinguish rule from accident" — no such consumer exists yet (migration
   decisions and Airbase intent-diffs are M4+/Goal-7 territory). Shipping ahead of the
   trigger would make the falsifier decorative.
2. **Reflexive §3 prohibits speculative schema growth** ("walls must be hit, not
   imagined") — the same rule that keeps "Fineract might need X" out of v1.0 applies
   to our own pet field.
3. **The concrete instance doesn't yet pinpoint a claim.** The reference-equality
   quirk (`itemQuantity != BigDecimal.ZERO`, Goal1-handoff §4) is preserved
   *sub-claim-level* — no current claim asserts the quirk itself, so a v1.0
   `fidelity:` marker would have nothing truthful to mark. When a consumer forces the
   delta, the field lands together with the claim-level test case it marks.
4. **The deferral is cheap and reversible by design**: an optional field is a minor
   version (IR-SCHEMA.md §13 lists it under "explicitly open"), so no future cost is
   incurred by waiting — whereas a speculative field frozen into v1.0 could only be
   removed by a major version.
