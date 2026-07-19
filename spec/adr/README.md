---
confidence: analyzed
related: [spec/analysis/Airlift-reflexive.md, spec/planning/Airlift-m2-freeze.md, spec/index.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/analysis/IR-analysis.md, spec/planning/Goal1-handoff.md]
trajectory: >
  The materialized AADR register (seeded from Airlift-reflexive.md §5,
  materialized 2026-07-19, ahead of the M2 schedule). New load-bearing
  decisions add an entry at merge time; falsifier observations are appended
  to the owning AADR when they occur. Never retired — this IS the WHY record.
---

# spec/adr/ — the self-ADR (AADR) register

Airlift's reflexive discipline applied to itself: every load-bearing design decision
gets an ADR with the decision, the alternatives actually considered, the evidence that
decided it, and — the part ordinary ADRs omit — a **falsifier**: the observation that
would reopen it. An ADR without a falsifier is marketing; an ADR with one is a claim.

## Register rules (from `spec/analysis/Airlift-reflexive.md` §5, normative)

1. New load-bearing decisions require an AADR at merge time (M2 delta-process
   checklist item).
2. Falsifier observations are recorded on the AADR when they occur, **whether or not
   they trip it** (append to the *Falsifier observations* section — dated, with
   evidence pointer).
3. A tripped falsifier obligates a **review**, not automatically a reversal.
4. Each entry is tagged **load-bearing** (chosen against alternatives, for reasons
   that still apply) or **incidental** (an accident of the spike that survived because
   nothing pushed back). Incidental decisions are the ones a freeze must not
   accidentally canonize.

## The register

| ID | Decision | Tag | Status |
|---|---|---|---|
| [AADR-001](AADR-001-constrained-gherkin.md) | Claims = constrained Gherkin + decision tables | load-bearing | active |
| [AADR-002](AADR-002-closed-glossary.md) | Closed glossary, mandatory noun resolution | load-bearing | active |
| [AADR-003](AADR-003-atom-per-file-graph-as-view.md) | One atom per file; flat YAML; graph is a view | principle load-bearing; flatness incidental | active |
| [AADR-004](AADR-004-operational-sufficiency-proofs.md) | Operational sufficiency proofs E1–E4 as the quality gate | constitutive | active |
| [AADR-005](AADR-005-open-claim-kind-stack.md) | IR = open stack of typed claim-kinds; v1 populates one | stack load-bearing; population incidental | active |
| [AADR-006](AADR-006-fused-coevolution-until-freeze.md) | Fused co-evolution of IR and pipeline until the freeze; deltas after | load-bearing | expired 2026-07-19 (M2 freeze executed) |
| [AADR-007](AADR-007-copilot-executes-claude-authors.md) | Author-role vs execution-harness role split; current Claude/Copilot binding is a deployment detail | role split load-bearing; product binding incidental | active |
| [AADR-008](AADR-008-compile-errors-only-repair.md) | Blind repair feedback = compile errors only | load-bearing | active |
| [AADR-009](AADR-009-hand-built-harness.md) | Per-seam hand-built harness (fixture DSL + contract doc) | incidental | active — known cost center |
| [AADR-010](AADR-010-bug-compatible-fidelity.md) | Bug-compatible extraction without a queryable fidelity marker | incidental — known debt | active — deferred from v1.0 at M2; v1.1 candidate |
| [AADR-011](AADR-011-junit3-spine-binding.md) | JUnit-3-idiom spine binding via `@AirliftClaim` | incidental | active |
| [AADR-012](AADR-012-stable-ids-fragment-binding.md) | Stable claim IDs (supersede, never rename); fragment identity = symbol+role, lines are evidence-at-commit | load-bearing | active |

Status legend: **active** (decision stands) · **expires at \<event\>** (has a designed
end) · **reopened** (falsifier tripped, review pending) · **superseded by AADR-NNN**.

Out-of-band fitness signals that feed falsifiers (reflexive C2 — signals the
optimization loop does not control): cross-model E1 (R2 → AADR-004/007), full-seam
mutation score (R1 → AADR-004), second-target friction log (R3 → AADR-006, R4 →
AADR-001/005), harness cost accounting (R5 → AADR-009). Scheduled basis re-derivation
(C3, ~M3 and ~M7) can reopen any entry.
