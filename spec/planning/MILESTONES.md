---
confidence: analyzed
related: [spec/planning/goals.md, spec/analysis/Airlift-reflexive.md, spec/index.md]
sources: [spec/planning/goals.md, spec/planning/Goal1-handoff.md, spec/drafts/Airlift-article.md]
trajectory: >
  The roadmap M1-M9; each milestone's section gains its verdict + evidence
  pointer at completion. Consumed by every planning session.
---

# Airlift — Milestones

Roadmap from the proven Goal 1 spike to full Airlift as envisioned in
`spec/drafts/Airlift-article.md`. Goals are defined in `spec/planning/goals.md`; current state in
`spec/planning/Goal1-handoff.md`. Status date: **2026-07-19**.

Reflexive discipline: Airlift's own design decisions are recorded as falsifiable
self-ADRs — the WHYs, the load-bearing/incidental tags, and the local-optimum
countermeasures live in `spec/analysis/Airlift-reflexive.md` (normative for M2/M3).

Standing invariants (carry through every milestone — do not relax):

* **Target-agnostic core.** Nothing target-specific ever lands in `pipeline/` or
  `ir-spec/`; targets are plugins under `tests/targets/<name>/`.
* **Copilot+Opus executes; Claude authors.** Delivered artifacts must run under the
  `copilot -p` BYOK harness.
* **Blind means blind.** IR-only test generation never sees code; repair feedback is
  compile errors only.
* **Red blind test ⇒ fix the IR (or harness), never the test**, then rerun with a fresh
  agent.
* **Nondeterminism never sits on the truth path.** Claims compile to tests
  deterministically; LLMs propose, validators and humans ratify.

Dependency shape:

```
M1 (done) ─▶ M2 (freeze/harden) ─▶ M3 (second target) ─▶ M8 (glossary federation — Goal 6)
                    │                                        │
                    ├─▶ M4 (PR history — Goal 2)             │
                    ├─▶ M5 (architecture — Goal 3, unblocked, any time)
                    ├─▶ M6 (Airlift MCP — Goal 4)            │
                    └─▶ M7 (drift spine — Goal 5)            │
                                   │                         │
                                   ▼                         ▼
                        M9 (productionalization — Goal 7: org rollout,
                            Airbase bridge, Maestro, per-team partitions)
```

---

## M1 — IR + code distillation, fused, on one slice ✅ COMPLETE (Goal 1)

The funded spike. Target-agnostic distillation pipeline + Airlift IR, co-evolved
against OFBiz `TaxAuthorityServices.rateProductTaxCalc` (tax-exemption seam), executed
end-to-end on Copilot CLI + Opus 4.8 BYOK.

**Exit criteria — all passed** (evidence: `tests/out/ofbiz-tax/RUN-REPORT.md`):

| Exit | Result |
|---|---|
| E1 blind IR-only testgen green vs real code | 35/35, enforced sandbox |
| E2 planted bugs caught and claim-named | 7/7 |
| E3 behavior-preserving refactor stays green | 675-line diff, 35/35 |
| E4 flipped exemption rule caught AND named | spine named the 4 TAX.EXEMPT claims |

---

## M2 — Freeze & harden the contract ✅ COMPLETE (2026-07-19)

Goal 2 consumes the claim schema as a fixed input, so the schema must become a
versioned, shipped artifact before anything builds on it. Also pays down known debt
from the spike.

**Executed 2026-07-19 — all exit criteria met** (evidence: `tests/runs/m2-freeze-report.md`):
schema v1.0 tagged + version-checked validators; `promote.py` promoted 27 claims to
`verified` / 3 `pinned` / 4 `extracted` with zero manual edits, idempotent (the "35
verified" below was a spec arithmetic error — 35 is the blind test-method count, the IR
has 34 claims of which the blind suite binds 27); AADR-012 added by the traceability
pass; distill repair loop demonstrated end-to-end (organic structural failure + seeded
version mismatch, repaired, green); `fidelity:` deferred per AADR-010; stage-4 external
runner shipped and demonstrated.

**Deliverables**

* `ir-spec/IR-SCHEMA.md` tagged **v1.0**; subsequent changes are versioned deltas with
  a changelog, never live renegotiation.
* Commit the airlift repo (currently uncommitted — needs user's go-ahead).
* **Claim-status promotion tool**: deterministic promotion `extracted → pinned →
  verified` driven by test reports (backfill = pinned, blind-green = verified). This is
  the seed of the M7 anti-drift spine.
* Debt: external-runner variant for stage 4 (match E1's determinism); exercise
  `run-pipeline.sh` repair loop end-to-end for the distill stage.

**Exit criteria**

* Schema v1.0 tagged; a full pipeline rerun on ofbiz-tax validates against it clean.
* Promotion tool run on existing E1/E2 reports yields 35 `verified` claims with zero
  manual edits.

**Detailed spec: `spec/planning/Airlift-m2-freeze.md`** (adds the self-ADR register and the
freeze escape hatches from `spec/analysis/Airlift-reflexive.md`).

## M3 — Second target: the generality proof

One slice proves the method; a second target proves the *pipeline*. Candidates:
Fineract (over-abstraction pathology — the opposite failure mode from OFBiz's
entanglement) or OFBiz `InvoiceServices.createInvoiceForOrder` (the "final boss").

**Deliverables**: one new `tests/targets/<name>/` plugin only — descriptor, seam brief,
harness + fixture DSL, mutations. Pipeline and ir-spec untouched.

**Exit criteria**

* E1–E4 all pass on the new target.
* `git diff` over `pipeline/` and `ir-spec/` between start and finish is empty — or
  every delta is a justified, versioned schema change (a discovered claim-kind gap,
  which is signal, not failure).

**Detailed spec: `spec/planning/Airlift-m3-second-target.md`** (also the measurement milestone:
friction log, cost accounting, cross-model E1, mutation-sweep stretch — the out-of-band
fitness signals from `spec/analysis/Airlift-reflexive.md`).

## M4 — PR-history distillation (Goal 2)

Multilevel blame: deconstruct git's cumulative projection into layers, joined across
repos via the work-item hierarchy. Index eagerly, extract lazily; history testifies, a
human ratifies. Spec: `spec/drafts/Arlift-PR-history-distillation.md`. Grows from the
`provenance/*.yaml` stubs (git-only today).

**Deliverables**

* **Phase 1 — the Ledger** (one-time, deterministic, no LLM): SQLite index of
  PR → commits → files/symbols → work item → Story → Feature → Epic, cross-repo.
  Pointers only, never diff contents. Validate churn/coupling metrics against Code Maat.
* **Phase 2 — claim-driven mining** (on-demand, LLM): claim binding → blame → Ledger
  joins → bounded synthesis → **tier-3 inferred claim awaiting human ratification**.
* **Phase 3 — heat prioritization**: mine hotspots (churn × incident-linked ×
  claim-bound) first; cold history stays unmined.

**Detailed goal spec: `spec/planning/Airlift-goal-2-pr-history.md`.**

**Exit criteria**

* The Ledger answers the four required questions deterministically (which features
  touched this code; when was this functionality first bound to which feature; which
  repos a feature touched; how behavior changed by feature).
* For ≥5 claims from M1's IR, mining produces provenance narratives with correct
  commit/PR citations, landing as tier-3 inferred claims — never silently promoted.
* An IR gap found here ships as a versioned schema delta, not a live change.

## M5 — Architecture distillation (Goal 3)

Independent track — different source of truth (patterns/ADRs, not business rules),
different output (mustache pattern library), different validation (swapped architecture
leaves behavior invariant). Unblocked; run whenever. Grows the deliberate stub at
`tests/out/ofbiz-tax/ir/architecture/` (3 ADRs + 3 patterns) into its own pipeline.
**Detailed goal spec: `spec/planning/Airlift-goal-3-architecture.md`** (borrows Understand-Anything's
deterministic/LLM split, forced layer assignment, and reviewer-gate — never its
unverified narration).

**Deliverables**

* Pattern-extraction pipeline stage(s): unambiguous parameterized patterns (code /
  config / script snippets, mustache parameters), referenced from ADRs with concrete
  bindings.
* Conformance check: generated code validated against the pattern library.

**Exit criteria**

* **The swap test**: re-implement the distilled slice's plumbing under a materially
  different architecture drawn from the pattern library; the blind claim suite stays
  green untouched. (E3 was the first taste; this is the real thing.)
* Reverse direction: a deliberately pattern-violating change is flagged by
  conformance checking, naming the ADR/pattern violated.

## M6 — Airlift MCP (Goal 4)

Make the IR the cheapest path for agents: a query plane serving claims, glossary,
traceability, spine status, and provenance to coding agents and harnesses (Copilot,
OpenAI Symphony, Claude Code). This is the serving layer from the whitepaper's
component IV, scoped to the IR we actually have.
**Detailed goal spec: `spec/planning/Airlift-goal-4-mcp.md`** (borrows SocratiCode's tool
ergonomics, plugin/skills distribution, and project-identity pattern — never embedding
search on the truth path).

**Deliverables**

* MCP server over `tests/out/<target>/ir/`: query claims by ID/term/fragment, resolve
  glossary terms, fetch harness contract, report claim coverage and status.
* Answers ship with the paper trail — claim ID, status tier, owner, provenance — never
  as an oracle (the whitepaper's indemnity design: cite the record, don't assert).

**Exit criteria**

* An external coding agent, grounded **only** via Airlift MCP (no repo file access —
  E1-style enforced blindness), implements a small behavior change; the claim suite
  plus its new tests run green against real code.
* Tier discipline holds: verified, pinned, and inferred claims are visibly
  distinguished in every response.

## M7 — Drift detection & compensation (Goal 5)

The anti-drift spine — the whitepaper's non-negotiable. A KB that can be silently
wrong is worse than none; a stale claim is a build-breaking defect, not a
documentation chore.
**Detailed goal spec: `spec/planning/Airlift-goal-5-drift.md`** (borrows Understand-Anything's
fingerprint-gated incremental re-analysis as the cheap detection layer; the spine remains
the only adjudicator).

**Deliverables**

* CI-enforced spine: on every PR touching bound fragments, run the claim suite, check
  claim↔fragment link integrity, recompute claim coverage; block on unexplained red.
* Freshness/confidence decay + broken-link detection over the IR.
* **Compensation loop**: on drift, re-extract the affected seam, diff against stored
  claims, and open a proposed IR delta (behavior change → versioned claim update for
  ratification) or flag the code (unintended change → the E4 outcome). Distinguishing
  those two is the whole game.
* Round-trip direction 2 (disposability proof, per `spec/drafts/Airlift-IR.md`): regenerate
  the slice's code from IR alone; it passes the pre-existing suite — bisimulation
  demonstrated, Java demoted to projection.

**Exit criteria**

* A silently merged behavior change on a bound fragment turns CI red naming the
  violated claim (E4, but ambient and automatic rather than staged).
* A ratified claim change flows the other way: IR delta → failing test → code change →
  green, with provenance recorded.
* Regenerated-from-IR code passes the legacy behavioral suite on at least one slice.

## M8 — Glossary federation (Goal 6)

The whitepaper's cornerstone, promoted to its own milestone: the glossary as lingua
franca between AI and humans AND the bridge between Airlift and the org's other KBs —
foremost **Airbase** (= our per-team specialization of the Team Brain KB product;
tribal knowledge, wikis, PM docs, ongoing meetings). Sequenced
after M3 because federation needs ≥2 target glossaries to be provable (namespacing,
polysemy, merge governance — the IR-analysis pitfall-4 problem, faced head-on).

**Deliverables**

* Glossary namespacing + federation model (per-target/per-team partitions, shared core);
  merge/deprecation/ownership governance, exercised not just documented.
* **Term linter**: incoming text (claims, PR descriptions, Airbase notes) → canonical
  terms, or a "new term?" proposal — the whitepaper's ingestion gate, built.
* **Airbase bridge**: alias mapping between code-anchored Airlift terms and
  intent-anchored Airbase concepts; term-level provenance marks which side anchors each.

**Exit criteria**

* ofbiz-tax + second-target glossaries federate without collision; cross-target queries
  resolve.
* Linter catches seeded colloquialisms/ambiguous terms in a real doc round-trip;
  unknown terms produce proposals, never silent additions.
* One Airbase document linted end-to-end: every noun resolves to a canonical term or an
  open proposal.

**Detailed goal spec: `spec/planning/Airlift-goal-6-glossary.md`.**

## M9 — Productionalization (Goal 7; absorbs the former "M8 — KB productization")

The organizational deliverable. Sequenced last because every piece consumes M2–M8
outputs as stable contracts. Org reality: 100+ microservices, ~6 teams, hundreds of
thousands of lines, Conway-partitioned — so: **centralized persistent storage + MCP,
partitioned per team**; explicit responsibility split with **Airbase** (Airlift =
un-opinionated facts from code + history, semantics and trajectory; Airbase = distilled
engineer/PM intent; Airlift grounds Airbase and executes its vision); cooperation with
**Maestro** (Symphony-clone PR-lifetime harness) as the action plane; drift fought
proactively at scale; and a designed **carrot** — immediate day-1 value for onboarding
teams.

**Scope** (from `spec/drafts/Airlift-article.md`, updated for the Airlift/Airbase
split; each item becomes its own plan when funded):

* Two-layer store, centralized: MD + frontmatter as event log, derived graph/vector
  index as rebuildable projection; **partitioned per team** (Conway-honest), cross-team
  read, per-team write/ownership.
* Multi-tenant Airlift MCP over the partitioned store (M6 generalized).
* **Airbase integration**: Airlift as a grounding source for Airbase (cited claims with
  tiers); Airbase intent flows back as ratified IR deltas Airlift helps execute. The
  whitepaper's ingestion pipelines (meetings, specs, incidents) and three-tier
  truth-status discipline land on the Airbase side of the split.
* **Maestro integration** (Symphony-clone PR harness): claim grounding + drift-spine
  gating + provenance updates inside the automated PR lifetime; violated claims route
  to the human turn.
* The **carrot**: day-1 value for onboarding teams before full distillation (Ledger
  archaeology, blast-radius reports, onboarding answers); Obsidian lens + daily/
  on-demand HTML reports (the adoption wedge).
* Governance: per-team instances, single ownership, tiered answers with intact
  liability chains (system-of-record mandate); stale-claim SLA and drift dashboards.

**Exit criterion**: one real team runs one real feature through the loop — a
Maestro-driven, Airlift-grounded agent implements it, the spine verifies it, ticket and
provenance update themselves, Airbase cites Airlift claims in the feature's intent doc —
and asking Airlift beats asking a colleague on the whitepaper's metrics (time-to-answer,
drift count, agent task success).

**Detailed goal spec: `spec/planning/Airlift-goal-7-productionalization.md`.**
