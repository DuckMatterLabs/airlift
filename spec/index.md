---
confidence: proven
related: [spec/log.md, CLAUDE.md]
sources: [spec/planning/, spec/analysis/, spec/drafts/]
trajectory: >
  Living map - every new spec doc adds a row; restructure only alongside an
  actual folder change.
---

# spec/ index — the semantic map

How the documents relate. Convention: `drafts/` holds the WHY (original human prose —
protected, never regenerated); `planning/` holds the WHAT-NEXT (goals, milestones,
state); `analysis/` holds the CRITIQUE (earned assessments, including of ourselves).
Evidence lives outside spec/: `tests/out/<target>/` (run products + RUN-REPORT), `tests/runs/`
(logs), `ir-spec/` (normative schema).

## drafts/ — original prose (tier-3-human: source material, guard it)

| Doc | What it is |
|---|---|
| `Airlift-article.md` | The whitepaper: full vision — KB-as-system-of-record, glossary, tiers, indemnity/moral-crumple-zone design, adoption, components I–IV. Revised 2026-07-19 for the Airlift/Airbase split |
| `Airlift-IR.md` | IR design rationale: why not pseudo-code/prose, the typed claim-kind stack, round-trip/bisimulation, boilerplate classification, truth tiers |
| `Airlift-code-distillation.md` | Code-distillation discussion (Goal 1 background) |
| `Airlift-arch-distillation.md` | Architecture-as-distinct-from-system thesis (Goal 3 seed) |
| `Arlift-PR-history-distillation.md` | Multilevel blame, Ledger, index-eagerly/extract-lazily, MSR prior art (Goal 2 seed) |

## planning/ — goals, milestones, state (the executable plans)

| Doc | What it is |
|---|---|
| `MILESTONES.md` | The roadmap M1–M9: dependency shape, standing invariants, per-milestone exits |
| `goals.md` | The seven goals, one paragraph each, with spec pointers |
| `Goal1-handoff.md` | The operational bible: state at M1 completion, how to run everything, gotchas §4; **§8 = M2 addendum + Copilot takeover briefing** (promotion workflow, frozen-schema discipline, M3 notes). Convention: each completed goal produces a `Goal<N>-handoff.md` here |
| `Airlift-goal.md` | Goal 1 spec (COMPLETE — evidence: `tests/out/ofbiz-tax/RUN-REPORT.md`) |
| `Airlift-m2-freeze.md` | M2: schema v1.0 freeze + self-ADR register + promotion tool + debt (COMPLETE 2026-07-19 — evidence: `tests/runs/m2-freeze-report.md`) |
| `Airlift-m3-second-target.md` | M3: generality proof (Fineract recommended) + the measurement milestone (friction log, cost, cross-model E1, mutation sweep) |
| `Airlift-goal-2-pr-history.md` | Goal 2 / M4: the Ledger + claim-driven mining + heat prioritization |
| `Airlift-goal-3-architecture.md` | Goal 3 / M5: pattern library + ADRs + conformance + the swap test |
| `Airlift-goal-4-mcp.md` | Goal 4 / M6: progressive-disclosure MCP, clerk-not-oracle tiers |
| `Airlift-goal-5-drift.md` | Goal 5 / M7: two-layer drift spine (fingerprints detect, tests adjudicate) + compensation loop + disposability proof |
| `Airlift-goal-6-glossary.md` | Goal 6 / M8: glossary federation — namespacing, term linter, the Airbase bridge (code-anchored vs intent-anchored terms) |
| `Airlift-goal-7-productionalization.md` | Goal 7 / M9: org rollout — partitioned central store + multi-tenant MCP, Airlift/Airbase responsibility split, Maestro integration, the onboarding carrot, drift at scale |

Reading order for a new contributor: `goals.md` → `MILESTONES.md` → the latest
`Goal<N>-handoff.md` → the spec for whatever milestone is next.

## analysis/ — critical assessments (earned, dated, falsifiable)

| Doc | What it is |
|---|---|
| `IR-analysis.md` | Critical analysis of Airlift's IR + comparison with Understand-Anything (borrow: structure, fingerprints, reviewer-gates; refuse: unverified narration). Also the MCP progressive-disclosure design |
| `socraticode-analysis.md` | Comparison with SocratiCode (borrow: MCP ergonomics, distribution, project identity; refuse: embedding search on the truth path). The three-way design-space map |
| `Airlift-reflexive.md` | Airlift on Airlift: the WHY record (D1–D7), evolution mechanisms, local-optimum risks R1–R5 + countermeasures C1–C5, seed self-ADR register AADR-001…011. **Normative for M2/M3** |

## adr/ — the self-ADR (AADR) register (decisions with falsifiers)

| Doc | What it is |
|---|---|
| `adr/README.md` | Register index + rules (new load-bearing decision ⇒ AADR at merge; falsifier observations appended in place; tripped falsifier ⇒ review, not auto-reversal) |
| `adr/AADR-001…012-*.md` | One decision each: 001–011 seeded from `analysis/Airlift-reflexive.md` §5 (materialized 2026-07-19, ahead of the M2 schedule); 012 added at the M2 freeze (identity scheme, register rule 1). Each: decision, tag (load-bearing/incidental), alternatives, evidence, falsifier, observations log |

## Cross-cutting threads (where to look for a topic)

* **Truth tiers / indemnity**: `drafts/Airlift-article.md` §Indemnity → served by
  `planning/Airlift-goal-4-mcp.md`, enforced by `planning/Airlift-goal-5-drift.md`.
* **Claim-kind stack** (only one kind implemented so far): `drafts/Airlift-IR.md` →
  reflexive D5 → M2 freeze scope.
* **Completeness vs sufficiency**: `analysis/IR-analysis.md` pitfall 2 → reflexive R1 →
  M3 mutation-sweep stretch.
* **Borrowed prior art**: UA → Goals 3 & 5; SocratiCode → Goal 4; Code Maat/SZZ/
  CodeShovel → Goal 2.
* **Airbase relationship** (facts vs intent): `planning/Airlift-goal-7-productionalization.md`
  §split (normative statement) → glossary bridge in `planning/Airlift-goal-6-glossary.md`
  Phase C.
* **Session memory**: `log.md` (append-only, newest first).

## Housekeeping rules

* New spec docs go in the matching subfolder and get a row here — this index is the
  map; an unmapped doc is a lost doc.
* Every generated MD file carries frontmatter (`confidence`, `related`, `sources`,
  `trajectory`) — the template and field semantics are in `CLAUDE.md`.
* `sources/` holds captured external material (unindexed).
* Repo-relative paths everywhere.
