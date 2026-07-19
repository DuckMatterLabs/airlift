---
confidence: analyzed
related: [spec/adr/AADR-011-junit3-spine-binding.md, spec/planning/Airlift-m3-second-target.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/analysis/IR-analysis.md, spec/planning/Goal1-handoff.md]
trajectory: >
  Retired the day harness scaffolding generation (IR-analysis optimization 3)
  works; adjudicated by M3's cost accounting (R5). The economic-viability ADR.
---

# AADR-009 — Per-seam hand-built harness (fixture DSL + contract doc)

**Tag:** incidental (spike economics). **Status:** active — known cost center.
**Origin:** reflexive register; IR-analysis pitfall 1.

## Decision

Each target seam gets a hand-built harness: fixture DSL (`TaxFixture`), the
mechanics-only `harness-contract.md` blind agents write tests against, the
`@AirliftClaim` spine reporter, install/run scripts, and hand-planted mutations.
Nothing generates these today.

## Alternatives considered

None at spike time — building one excellent harness by hand was the fastest route to
proving the method. The decision is recorded precisely because it was *not* chosen
against alternatives: it is the canonical incidental decision, and the freeze must
not canonize it.

## Evidence

The harness is the expensive, expert part — and exactly the part that isn't
generated (IR-analysis pitfall 1). Goal 1 covered one 772-line class; OFBiz has
hundreds of such seams. Until harness construction is itself distilled, Airlift is a
precision instrument, not a codebase-scale tool. One supporting design fact: the
contract doc carries mechanics only, no behavior (invariant 4), which is what makes a
*generated* harness conceivable without touching the truth path.

## Falsifier

R5 — the harness-cost assumption: cost is currently treated as a constant of nature.
M3's cost accounting adjudicates: if per-seam harness cost does not drop materially
with experience and scaffolding, the amortization story needs a redesign, not a
delta — Airlift's economic claim fails at codebase scale regardless of how green the
proofs are. Conversely, harness scaffolding generation succeeding (IR-analysis
optimization 3) retires this AADR outright.

## Falsifier observations

* 2026-07-19 — n=1; no cost curve exists yet. The second target produces the first
  real data point.
