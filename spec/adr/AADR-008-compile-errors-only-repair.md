---
confidence: proven
related: [spec/adr/AADR-004-operational-sufficiency-proofs.md, CLAUDE.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/planning/Goal1-handoff.md, tests/out/ofbiz-tax/RUN-REPORT.md]
trajectory: >
  Standing invariant 3/4; frozen with the proof methodology at M2. Reopens
  only on evidence of systematic under-repair with a non-leaking alternative.
---

# AADR-008 — Blind repair feedback is compile errors only

**Tag:** load-bearing. **Status:** active. **Origin:** reflexive D7 / standing
invariants 3–4.

## Decision

During blind test generation, repair rounds may feed back COMPILE errors only —
never test failures, never expected-vs-actual values. And when a blind test runs red
against real code, the fix goes into the IR (or harness), never the test; the rerun
uses a FRESH agent.

## Alternatives considered

* **Feed back test failures / expected-vs-actual** — rejected: leaks behavior into
  "blind" tests, quietly converting the sufficiency proof into a fitting exercise.
  The suite would converge on the code, not on the IR.
* **Patch the failing test** — rejected: destroys the evidentiary value of red; a red
  blind test is the *method* (it names an IR ambiguity or harness bug, which is the
  thing to fix).
* **Reuse the same agent after IR repair** — rejected: the agent's context is
  contaminated by its own failed attempt; only a fresh agent re-proves sufficiency.

## Evidence

The loop ran once in anger (Goal1-handoff §5.3): first blind run 30/35; the 5 reds
decomposed into 3 IR ambiguities (VAT trigger = product's tax-inclusive catalog
price, not store VAT-display; containment requires an address that itself passes the
address guard) + 2 harness bugs (incl. FK-order). Claims were repaired, tests never
touched, fresh agent reran → 35/35. Compile-only feedback was sufficient to get
generated suites compiling without ever leaking behavior.

## Falsifier

Evidence that compile-only feedback systematically under-repairs (blind suites that
compile but structurally can't exercise the claims, across targets) *while a
non-leaking richer channel exists* (e.g., harness-mechanics diagnostics that carry no
behavior). Both conditions required — under-repair alone doesn't justify opening a
leak.

## Falsifier observations

* 2026-07-19 — none tripped; one-seam sample.
