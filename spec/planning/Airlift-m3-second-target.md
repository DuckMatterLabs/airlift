---
confidence: proposed
related: [spec/planning/Airlift-m2-freeze.md, spec/analysis/Airlift-reflexive.md, Fineract]
sources: [spec/planning/MILESTONES.md, spec/planning/Airlift-goal.md, spec/analysis/Airlift-reflexive.md]
trajectory: >
  Executes after M2; produces the falsifier evidence for reflexive R1/R2/R3/R5;
  friction log becomes the schema-delta intake queue.
---

# Airlift M3 spec — Second target: the generality proof

Status: NOT STARTED. Date: 2026-07-19.
Prerequisite: **M2** (frozen schema — otherwise "pipeline unchanged" is unfalsifiable,
because the pipeline's contract would still be soft).
Background: `MILESTONES.md` §M3, `spec/planning/Airlift-goal.md` (target dossiers),
`spec/analysis/Airlift-reflexive.md` — this milestone *is* countermeasure C2's main instrument:
it generates the out-of-band fitness signals (friction log, cost accounting, cross-model
E1, mutation sweep) that keep Airlift honest about its own generality.
Execution harness: Copilot CLI (`copilot -p`) + Opus BYOK, unchanged.

## Goal summary

One slice proves the *method*; a second target proves the *pipeline*. The deliverable is
deliberately boring — one new `tests/targets/<name>/` plugin, nothing else — and the measure is
everything that *wasn't* supposed to change: `pipeline/` and `ir-spec/` diffs, prompt
bending, and harness cost. M3 is equally a **measurement milestone**: it produces the
evidence that several reflexive falsifiers (R1, R2, R3, R5) are waiting on.

## Target selection

Two candidates from `Airlift-goal.md`; the choice is itself signal-bearing:

* **Fineract** (recommended): the *opposite pathology*. OFBiz is entanglement (rule,
  persistence, plumbing as one tissue); Fineract is over-abstraction (rules hidden under
  class hierarchies, deep ORM nesting, abstraction interfaces). A pipeline tuned on
  entanglement may reward the wrong extraction moves on abstraction (reflexive R3) —
  which is exactly why Fineract is the stronger generality probe. It also varies
  everything incidental: different repo, build (Gradle/Spring vs OFBiz's bespoke
  framework), test idiom (JUnit 4/5/Spring vs OFBiz's JUnit-3 suites — directly exercises
  AADR-011's claim that spine binding is a target-plugin concern), data setup (no H2
  seed-tier gotchas, different fixture reality). Candidate seam: an interest-calculation
  or loan-charge rule — crisp in one sentence, buried under layers (the mirror image of
  the tax seam's criteria).
* **OFBiz `InvoiceServices.createInvoiceForOrder`** (the "final boss", 3,863 lines):
  same pathology, bigger. Valuable later as a *scale* probe, but it shares the harness,
  fixture DSL genre, and framework quirks with M1 — it would prove endurance, not
  generality. Sequence it after Fineract, or as M3.5 if scale evidence is wanted early.

Final seam choice happens at execution start with a one-line justification (the Goal-1
discipline: refine before execution, then run a fixed plan).

## Method

1. **Target plugin authoring** — `tests/targets/fineract-<seam>/`: `target.env` (paths, build,
   test commands), `seam.md` brief, `harness/` (fixture DSL, base case, claim-binding
   mechanism appropriate to the target's test stack, contract doc, smoke tests, install
   + run scripts, spine reporter config), `mutations/` (planted bugs, occurrence-checked
   as in M1). Reuse `report-claims.py` and the pipeline unchanged; anything that *can't*
   be reused unchanged goes in the friction log.
2. **Pipeline replay** — `run-pipeline.sh tests/targets/fineract-<seam> all`: map → testscape
   → catalog → backfill → distill, each stage through its own validator + repair loop
   (no manual driving this time — that determinism was M2 debt, now paid).
3. **Exit proofs** — E1 blind (fresh sandboxed agent, IR + harness contract only), E2
   mutations, E3 refactor, E4 flip: the full ladder, new target.
4. **Measurements** (first-class outputs, not by-products):
   * **Friction log** — every point where a prompt, validator, or schema element needed
     target-specific bending, with resolution (parameterized? target plugin? schema
     delta?). This is R3's falsifier data.
   * **Cost accounting** — hours + tokens split harness vs pipeline vs proofs, compared
     to M1. This is R5's falsifier data (does harness cost drop with experience?).
   * **Cross-model E1** (reflexive R2, cheap, do it here): after the normal E1 passes,
     rerun blind generation with a *different model family* under the same sandbox, on
     both targets. Materially redder cross-model results ⇒ prior-leakage confirmed ⇒
     IR tightening work item.
   * **Stretch — full-seam mutation sweep** (reflexive R1): systematic mutants over the
     seam (not 7 curated bugs); caught-fraction = the IR's first honest completeness
     score. Stretch because mutant tooling for the target stack may be nontrivial;
     if skipped, record it as an open falsifier, not as done.

## Deliverables

* `tests/targets/fineract-<seam>/` — complete plugin (the only new production code).
* `tests/out/fineract-<seam>/` — full pipeline products + `RUN-REPORT.md` in the M1 genre.
* `tests/exit/` runners generalized only if generalization is target-agnostic (else per-target
  variants live in the plugin).
* `friction-log.md`, cost accounting section in the run report, cross-model E1 results;
  mutation-sweep results or an explicit skip record.
* AADR updates: falsifier observations recorded on AADR-007/-009/-011 (and any tripped).

## Exit criteria

1. **E1–E4 all pass on the new target** (E1 with enforced sandbox, fresh-agent rerun
   after any IR repair — the M1 discipline verbatim).
2. **`git diff` over `pipeline/` and `ir-spec/` between milestone start and finish is
   empty** — or every delta is a justified, versioned schema/pipeline change with a
   changelog entry and (where load-bearing) an AADR. A discovered claim-kind gap is
   signal, not failure — but it lands via the M2 delta process, never as live
   renegotiation.
3. Friction log and cost accounting delivered; cross-model E1 executed on both targets
   with results recorded against R2.
4. The new plugin contains zero copies of pipeline logic (grep-level check: no forked
   prompts, no duplicated validators — parameterization or plugin extension points only).

## Non-goals

* Improving M1's target beyond what generalization forces.
* Harness scaffolding generation (IR-analysis optimization 3) — M3 *measures* the
  harness cost that motivates it; building it is its own later work item, informed by
  two data points instead of one.
* New claim-kinds on speculation. If Fineract's seam genuinely needs one (reflexive R4
  says eventually pick a seam that does), it arrives as an evidenced minor delta.
