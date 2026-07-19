---
confidence: analyzed
related: [spec/adr/AADR-009-hand-built-harness.md, spec/planning/Airlift-m3-second-target.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/planning/Goal1-handoff.md]
trajectory: >
  Expected to be restructured at M3: the second target's test stack forces
  the binding mechanism down into the target plugin where it belongs.
---

# AADR-011 — JUnit-3-idiom spine binding via `@AirliftClaim` annotations

**Tag:** incidental (OFBiz's harness reality). **Status:** active.
**Origin:** reflexive register.

## Decision

Claim↔test binding uses `@AirliftClaim` annotations on JUnit-3-idiom tests
(constructor `super(name)`, `public void testXxx()`), with `report-claims.py`
reading test results + annotations to produce the spine report ("TAX.EXEMPT.ZEROING
violated," not "testCase47 failed").

## Alternatives considered

None — JUnit 3 is what OFBiz's test infrastructure runs; the idiom's constraints
(suite-level data rollback with a shared delegator → namespace-unique fixtures,
find-before-create for fixed-ID records) are documented target gotchas
(Goal1-handoff §4), not choices. What *is* a choice, and load-bearing beyond this
AADR: the **spine principle** — red tests name violated claims semantically. That
principle lives with AADR-004; only the binding *mechanism* is incidental here.

## Evidence

The binding worked under every proof: E2's per-mutation reports named the right
claims (often exactly one); E4 named the four flipped TAX.EXEMPT claims. The
annotation survives refactors (E3) because it binds to claims, not to code structure.

## Falsifier

The second target's test stack (e.g., Fineract's JUnit 4/5, or anything non-JVM)
can't or shouldn't carry this mechanism — which forces the binding into
`tests/targets/<name>/` where it belongs, leaving the IR side of the contract
(claim IDs, statuses, spine report format) target-agnostic. This falsifier tripping
is the *expected and desired* outcome; the AADR exists so the migration is a
recorded restructuring, not silent drift.

## Falsifier observations

* 2026-07-19 — not yet tripped; single JVM target.
* 2026-07-19 (M2) — mechanism/principle split exercised without the falsifier
  tripping: the promotion tool (`pipeline/promote.py`) consumes only target-neutral
  machine-readable spine files (IR-SCHEMA.md §8, emitted by the target's
  `report-claims.py --spine-out`); annotation parsing never left the target plugin.
  The IR-side spine contract is now schema-frozen, so a second target swaps the
  mechanism without touching the core — the migration path this AADR anticipated.
