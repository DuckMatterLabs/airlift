---
confidence: proven
related: [spec/adr/AADR-007-copilot-executes-claude-authors.md, spec/adr/AADR-008-compile-errors-only-repair.md, spec/planning/Airlift-m3-second-target.md]
sources: [spec/analysis/Airlift-reflexive.md, tests/out/ofbiz-tax/RUN-REPORT.md, spec/analysis/IR-analysis.md, spec/sources/DigitalTwin.pdf]
trajectory: >
  Constitutive — the project's identity decision. Falsifier probes scheduled
  at M3: cross-model E1 (R2) and full-seam mutation sweep (R1). CDT Challenge
  XI is external validation of the methodology; cite it when defending E1-E4.
---

# AADR-004 — Operational sufficiency proofs (E1–E4) as the quality gate

**Tag:** constitutive (load-bearing beyond reversal — this is what Airlift *is*).
**Status:** active. **Origin:** reflexive D4.

## Decision

The IR's losslessness is not a format property but a round-trip demonstration
(bisimulation): a blind agent that has never seen the source generates tests from the
IR alone, and those tests must (E1) go green against real code, (E2) go red against
mutations naming the violated claims, (E3) survive behavior-preserving refactors,
(E4) catch and *name* deliberate behavior flips.

## Alternatives considered

* **Argued sufficiency** (schema review, human sign-off, LLM self-critique) — rejected:
  that is UA-style unverified narration; nothing in such a pipeline can detect a
  subtly wrong artifact, because nothing executes.
* **Traceability-only warrant** (every claim links to evidence, humans curate) — the
  Code Digital Twin model; rejected as a ceiling: a wrong rationale card survives
  until a human notices. "A KB that can be silently wrong is worse than none."

## Evidence

All four proofs passed on OFBiz tax (`tests/out/ofbiz-tax/RUN-REPORT.md`): E1 35/35
blind (sandbox empirically verified); E2 7/7 mutations caught, correct claims named
each time (M2 → exactly `THRESHOLD-MIN-ITEM-PRICE`); E3 675-line refactor, 10
extracted methods, 35/35 green; E4 spine named the four TAX.EXEMPT claims. The first
blind run's 30/35 → repair → fresh-agent 35/35 loop is the method working as
designed: the IR earns correctness rather than asserting it.

External validation: CDT's Challenge XI (realistic evaluation — mutation, constraint
preservation, longitudinal robustness) independently calls for exactly this proving
ground; Goal 1 is an existence proof for that research roadmap.

## Falsifier

Cross-model E1 or a full-seam mutation sweep shows the proofs passing while the IR is
materially ambiguous or incomplete (risks R1, R2). Two known gaps, stated honestly:

* **Sufficiency ≠ completeness (R1):** E2's mutations were planted where claims
  already existed; a mutation in un-claimed logic would sail through. Full-seam
  systematic mutation testing is the honest completeness measure (M3 stretch exit).
* **Double-LLM prior leakage (R2):** extractor and blind generator share Opus priors;
  an ambiguous claim can pass because both ends fill the gap identically. Cross-model
  E1 is cheap and scheduled into M3. If it goes materially redder, "blind" was
  partially theater.

## Falsifier observations

* 2026-07-19 — none tripped; both probes not yet run. R1/R2 recorded as known
  measurement gaps, not as evidence against the decision.
