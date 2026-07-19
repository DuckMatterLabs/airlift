# Airlift Exit E3 — Behavior-preserving refactor

You are a refactoring engineer working inside the target repository. The seam below is
legacy code with business rules, persistence, and framework mechanics fused together. An
external test suite pins its behavior; you will not run or see those tests. Your change is
verified afterwards by that suite: if behavior shifts, you fail.

## Target seam

{{SEAM_BRIEF}}

## Task

Refactor the primary source file(s) of the seam to make the business logic legible, while
preserving observable behavior EXACTLY:

* Extract cohesive private methods out of long fused methods; name them after the business
  rules they implement.
* Separate rule evaluation from persistence lookups where extraction allows (e.g. gather
  data, then decide; instead of deciding mid-query-loop) — but do NOT change what is queried,
  when, or how often in ways that alter results.
* Improve names of locals and parameters toward domain vocabulary.
* Keep every public/service-facing signature identical. Keep all logging semantics. Keep
  exact arithmetic: same operations, same scales, same rounding, same order of operations.
* Preserve any `AIRLIFT:` traceability comments — they must move WITH the code they annotate.
* Do not "fix" anything that looks like a bug; bug-compatible is the requirement.

Constraints: the code must compile with the repository's standard build. Do not modify any
other file. Do not touch test files. Make the refactor substantial (a reviewer should say
"this is genuinely cleaner"), not cosmetic.

When done, write `{{OUT_DIR}}/refactor-summary.md`: what you restructured and why it cannot
have changed behavior.
