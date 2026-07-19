---
confidence: analyzed
related: [spec/adr/AADR-005-open-claim-kind-stack.md, spec/drafts/Airlift-IR.md, TAX.EXEMPT.ZEROING]
sources: [spec/analysis/Airlift-reflexive.md, spec/drafts/Airlift-IR.md, tests/out/ofbiz-tax/RUN-REPORT.md]
trajectory: >
  Frozen into schema v1.0 at M2; falsifier probed deliberately at M3+ via a
  seam that should require a state-machine or property claim-kind (risk R4).
---

# AADR-001 — Claims are constrained Gherkin + decision tables

**Tag:** load-bearing. **Status:** active. **Origin:** reflexive D1.

## Decision

Behavior claims are written in constrained Gherkin (closed-vocabulary Given/When/Then)
plus decision tables — not pseudo-code, not free prose.

## Alternatives considered

* **Pseudo-code** — rejected *a priori* (`spec/drafts/Airlift-IR.md`): unexecutable yet
  code-shaped, so it re-imports the incidental structure of the source it was meant to
  abstract away.
* **Free prose** — rejected: puts a generative LLM step on the truth path — the same
  claim yields different tests on different days, violating standing invariant 5
  (nondeterminism never sits on the truth path).

Constrained Gherkin is the narrow overlap: deterministic enough to compile to tests,
readable enough for human ratification, writable enough for LLM extraction.

## Evidence

E1's repair loop worked *at the claim level*: the 3 IR ambiguities found by the first
blind run (30/35) were fixable as text edits to claims, and a fresh agent then
reproduced the behavior correctly — 35/35 (`tests/out/ofbiz-tax/RUN-REPORT.md`).
Constrained bodies are dense enough that the MCP design (IR-analysis, layer 3) serves
them raw, with no summarization step.

## Falsifier

A seam whose behavior cannot be expressed without prose escape hatches (risk R4 — the
expressiveness ceiling as invisible selection bias: temporal properties, concurrency,
cross-service invariants, probabilistic behavior have no claim-kind today). Note the
falsifier interacts with AADR-005: the sanctioned response to an inexpressible seam is
a *new claim-kind* (minor delta), not loosening Gherkin. This AADR reopens only if the
behavior fits no addable kind.

## Falsifier observations

* 2026-07-19 — none tripped. Standing caution recorded: the tax seam happened to be
  expressible almost entirely as behavior rules + decision tables (reflexive D5), so
  the ceiling is untested. M3+ seam selection must deliberately probe it (R4).
