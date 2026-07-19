---
confidence: analyzed
related: [spec/adr/AADR-005-open-claim-kind-stack.md, spec/planning/Airlift-m2-freeze.md, spec/planning/Airlift-m3-second-target.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/planning/goals.md, tests/out/ofbiz-tax/RUN-REPORT.md]
trajectory: >
  Expires by design at M2: co-evolution ends, versioned deltas begin. The M3
  friction log adjudicates whether the delta process is fast enough.
---

# AADR-006 — Fused co-evolution of IR and pipeline until the freeze; deltas after

**Tag:** load-bearing as an epistemic strategy — with a designed expiry.
**Status:** expired 2026-07-19 (M2 freeze executed: schema tagged v1.0, delta regime
in force — `ir-spec/CHANGELOG.md`, `ir-spec/IR-SCHEMA.md` §11). **Origin:** reflexive D6.

## Decision

Through Goal 1, the IR schema and the pipeline evolved *together*, each change driven
by hitting a real wall in a real seam (goals.md's Goal-1 argument). At M2 this mode
ends: the schema freezes at v1.0, and the same empiricism must arrive through
versioned deltas triggered by new-target friction (M3's friction log is the intake
queue). Not sanctioned afterwards: silent renegotiation (the freeze exists to end
it), and schema growth from speculative generality.

## Alternatives considered

* **Design the IR in isolation, then implement** — rejected: the schema would encode
  imagined behaviors instead of earning its shape against real entanglement.
* **Co-evolve forever** — rejected: perpetual renegotiation is exactly the silent
  drift Airlift exists to kill; downstream consumers (Goal 2's Ledger, the MCP) need
  a fixed contract.

## Evidence

The E1 red→IR-repair→fresh-agent loop is the method working as designed: 3 claim
ambiguities surfaced only because a blind consumer hit them, and the fixes went into
the schema's actual usage patterns. The IR's final shape (constrained Gherkin,
closed glossary, decision tables) was not the starting design — it was earned.

## Falsifier

The M3 friction log shows the delta process is too slow or too coarse for real target
friction — i.e., the second target needs schema changes at a rate or granularity the
versioned-delta machinery can't deliver, and people start routing around it. (That
would also be early warning for reflexive failure mode B: all-deltas-all-the-time is
greedy search; the chartered escape hatches are major versions with migration +
re-proof, and the scheduled basis re-derivation C3.)

## Falsifier observations

* 2026-07-19 — none; the delta regime hasn't started (M2 not yet executed).
* 2026-07-19 (later, M2 executed) — the expiry fired as designed: schema frozen at
  v1.0 with version-checked validators on every stage; change process chartered in
  `ir-spec/IR-SCHEMA.md` §11; changelog seeded (`ir-spec/CHANGELOG.md`). The falsifier
  (delta process too slow/coarse) now awaits M3 friction-log evidence.
