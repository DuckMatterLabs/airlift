---
confidence: proposed
related: [spec/drafts/Airlift-arch-distillation.md, spec/analysis/IR-analysis.md, spec/planning/MILESTONES.md]
sources: [spec/drafts/Airlift-arch-distillation.md, spec/planning/Airlift-goal.md, spec/planning/MILESTONES.md, spec/analysis/IR-analysis.md]
trajectory: >
  Executes as M5 (unblocked, any time); pattern library consumed by Goal 5's
  regeneration proof and M9; ends in a Goal3-handoff.md.
---

# Airlift Goal 3 spec — Architecture distillation (Milestone M5)

Status: NOT STARTED (deliberate stub exists: `out/ofbiz-tax/ir/architecture/` — 3 ADRs +
3 mustache patterns). Date: 2026-07-19.
Prerequisites: none hard — the most independent track; runnable any time. M2's schema
freeze is advisable so architecture artifacts version cleanly alongside claims.
Background: `spec/drafts/Airlift-arch-distillation.md`, `spec/planning/Airlift-goal.md` (pattern-library
format), `MILESTONES.md` §M5, `spec/analysis/IR-analysis.md` (Understand-Anything comparison —
what to borrow and what to refuse).
Execution harness: Copilot CLI (`copilot -p`) + Opus BYOK for all generative stages.

## Goal summary

Architecture is NOT the system: the same business behavior can live on a totally
different substrate. Distill the architecture of a target slice into its own IR —
**parameterized pattern library + ADRs with concrete bindings** — with its own validation
regime, which is the mirror image of the claims':

* Claims are validated by *surviving architecture change* (E3).
* Architecture artifacts are validated by **the swap test**: re-implement the slice's
  plumbing under a materially different architecture drawn from the pattern library, and
  the blind claim suite stays green untouched. Plus the reverse: a deliberately
  pattern-violating change is flagged by conformance checking, naming the ADR/pattern.

The dividing line this enforces is Airlift's own: architecture can evolve or be swapped
without affecting business behavior, and business behavior can evolve within
architectural constraints — each side's tests are the other side's invariance check.

## Inputs (fixed contracts)

* The M1 slice and its verified blind suite (35 tests) — the swap test's invariant.
* `out/ofbiz-tax/ir/architecture/` stub — grow it, don't restart it.
* Pattern format (from `Airlift-goal.md`, fixed): unambiguous specs as code / config /
  terraform / script snippets, parameters in **mustache** syntax; ADRs reference patterns,
  optionally pinning concrete parameter values.

## Method

Same pipeline discipline as M1 (staged, `{{PLACEHOLDER}}`-parameterized prompts,
deterministic validators, ≤3 repair attempts), new stages:

### Stage A — structural evidence map (deterministic-first)

Before any LLM narration, compute the cheap, reproducible substrate:

* File inventory + import/dependency graph of the slice's reachable code (Java imports,
  entity-model XML references, service-definition XML wiring).
* **Layer assignment: every file to exactly one layer** (service contract / service impl /
  entity engine / entity model / UI template / test). Forced single membership is the
  discipline worth borrowing (see UA borrowings below): ambiguity in layer assignment is
  itself a finding — it marks the fused seams Airlift exists for.
* Output: `architecture/layer-map.yaml` + `architecture/dep-graph.yaml`, both regenerable
  and diffable.

### Stage B — pattern candidate mining

From the evidence map, enumerate recurring architectural shapes as candidates: e.g., for
OFBiz — "service defined in `services_*.xml` + static Java method with
`DispatchContext/Map` signature + `ServiceUtil.returnError` result convention",
"EntityQuery builder access pattern", "delegator-based transactional write". Each
candidate cites ≥2 concrete instances (paths + line ranges) — a pattern with one instance
is not a pattern. Output: `pattern-candidates.yaml`.

### Stage C — pattern authoring

For each accepted candidate: a mustache-parameterized snippet set
(`patterns/<name>/{pattern.md, *.java.mustache, *.xml.mustache, …}`) that is
**unambiguous** — rendering it with a concrete parameter binding must yield compilable
artifacts. Validator: render every pattern with its recorded example bindings and check
the result against the actual source instances (normalized diff / structural match). A
pattern that cannot regenerate its own examples fails the stage.

### Stage D — ADR extraction

ADRs capture the *decisions* (why this pattern, alternatives, consequences), referencing
patterns with concrete bindings. Consult git history/the Goal 2 Ledger where intent is
unclear (optional join — do not block on M4). Validator: referential integrity — every
ADR→pattern reference resolves, every binding's parameters match the pattern's declared
parameter set, every cited code location exists.

### Stage E — conformance checker

A deterministic tool (`pipeline/conform.py` or per-target variant): given a diff or a
file set, detect pattern instances and report violations naming the pattern and ADR.
Scope honestly: detection is per-pattern matchers (structural, ast-grep-style), not
general inference. This is the reverse-direction exit criterion's engine.

### Stage F — the swap test (validation)

Pick a materially different architecture for the slice's plumbing, expressed as an
alternative pattern set (e.g., replace inline `EntityQuery` calls with a repository
interface + implementation; or re-wire the service through a different dispatch
convention). A Copilot+Opus agent re-implements the plumbing from the pattern library +
claims-untouched constraint. Then: `airliftblind` suite runs green with **zero test
edits**, and the conformance checker confirms the new code matches the *new* pattern set.
E3 was the first taste (same architecture, restructured); this is the real thing
(different architecture, same behavior).

## Borrowed from Understand-Anything (and what is refused)

Per `spec/analysis/IR-analysis.md`: UA's strength is cheap, broad structure; its weakness is
unverified narration. Borrow the former, never the latter:

* **Deterministic-parse / LLM-semantics split** (UA's tree-sitter vs LLM division):
  Stage A is deterministic and regenerable; LLM effort is spent only where judgment lives
  (naming patterns, writing ADRs). Here ast-grep (proven polyglot in SocratiCode) is the
  natural tool for both Stage A extraction and Stage E matchers.
* **Forced single layer membership** (UA `architecture-analyzer`): every file exactly one
  layer; contested files are flagged as fused seams, not averaged.
* **Reviewer-gate pattern** (UA `graph-reviewer`): a dedicated validation pass with an
  explicit approve/reject verdict over referential integrity and completeness of the
  pattern/ADR graph (Stage D validator is exactly this, made deterministic).
* **Committed-artifact, no-LLM viewing**: layer map, dep graph, and pattern library are
  plain committed files a human can browse without any model in the loop; a Mermaid
  rendering of the layer/dep graph is a cheap UA-style dashboard nicety.
* **Refused**: UA-style narrated summaries as architecture truth. Everything borrowed
  lands *upstream* of the swap-test gate. No artifact is "architecture IR" until the swap
  test (forward) and conformance flagging (reverse) have exercised it — narration that
  nothing executes is exactly the failure mode this project exists to end.

## Deliverables

* `pipeline/prompts/` new stages (A–D generative parts) + validators; conformance tool.
* `out/ofbiz-tax/ir/architecture/`: layer map, dep graph, pattern library (≥5 real
  patterns with example bindings), ADRs (grown from the 3 stubs), all referentially valid.
* Swap-test artifacts under `out/ofbiz-tax/e5-swap/` (diff, agent logs, green run).

## Exit criteria

1. **Swap test passes**: materially different architecture from the pattern library;
   `airliftblind` 35/35 green, zero test edits; conformance checker validates the result
   against the new pattern set.
2. **Reverse direction**: a deliberately pattern-violating change (e.g., raw `delegator`
   call where the repository pattern is mandated) is flagged, naming the violated
   pattern and ADR.
3. Every pattern regenerates its own example instances (Stage C validator clean).
4. `pipeline/` additions are target-agnostic; all OFBiz specifics live in
   `targets/ofbiz-tax/` (pattern matchers may be target files; the conformance engine may
   not).

## Non-goals

* Whole-codebase architecture recovery (UA's territory). Scope is the distilled slice
  and the org-pattern library mechanism; breadth arrives with more targets.
* Architecture *prescription*. This goal distills what is; ADR-driven evolution
  proposals are downstream (M9 era).
